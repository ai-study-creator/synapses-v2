package com.synapses.orchestrator.slack

import com.synapses.orchestrator.app.ManagedMcpClient
import com.synapses.orchestrator.app.orchestratorRuntime
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.request.receiveChannel
import io.ktor.server.response.respondText
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.utils.io.readRemaining
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.io.readByteArray
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

private const val DEDUP_TTL_SECONDS = 60L

fun Application.slackEventsRoutes(
    signingSecret: String,
    signatureVerifier: SlackSignatureVerifier = HmacSlackSignatureVerifier(signingSecret),
    dedupStore: SlackEventDedupStore = SlackEventDedupStore(),
    processor: SlackAppMentionScheduler = CoroutineSlackAppMentionScheduler(),
) {
    routing {
        route("/slack") {
            post("/events") {
                val rawBody = call.receiveChannel().readRemaining().readByteArray()
                val bodyText = try {
                    decodeUtf8Strict(rawBody)
                } catch (_: CharacterCodingException) {
                    call.respondText("Invalid request encoding", status = HttpStatusCode.BadRequest)
                    return@post
                }

                val timestamp = call.request.headers["x-slack-request-timestamp"]
                val signature = call.request.headers["x-slack-signature"]
                if (timestamp.isNullOrBlank() || signature.isNullOrBlank() ||
                    !signatureVerifier.isValid(bodyText, timestamp, signature)
                ) {
                    call.respondText("Invalid Slack signature", status = HttpStatusCode.Forbidden)
                    return@post
                }

                if (call.request.headers["x-slack-retry-num"] != null) {
                    call.respondText("", status = HttpStatusCode.OK)
                    return@post
                }

                val payload = try {
                    SlackJson.parseToJsonElement(bodyText).jsonObject
                } catch (_: IllegalArgumentException) {
                    call.respondText("Invalid JSON payload", status = HttpStatusCode.BadRequest)
                    return@post
                } catch (_: SerializationException) {
                    call.respondText("Invalid JSON payload", status = HttpStatusCode.BadRequest)
                    return@post
                }

                if (payload.stringValue("type") == "url_verification") {
                    val challenge = payload.stringValue("challenge")
                    if (challenge == null) {
                        call.respondText("Missing challenge", status = HttpStatusCode.BadRequest)
                    } else {
                        call.respondText("""{"challenge":"${escapeJsonString(challenge)}"}""", ContentType.Application.Json)
                    }
                    return@post
                }

                if (dedupStore.isDuplicate(payload)) {
                    call.respondText("", status = HttpStatusCode.OK)
                    return@post
                }

                val event = payload["event"]?.jsonObjectOrNull()
                if (event == null) {
                    call.respondText("", status = HttpStatusCode.OK)
                    return@post
                }

                if (event.stringValue("type") == "app_mention") {
                    if (event.isBotEvent()) {
                        call.respondText("", status = HttpStatusCode.OK)
                        return@post
                    }
                    processor.schedule(event, call.application.orchestratorRuntime.mcpClient)
                    call.respondText("", status = HttpStatusCode.OK)
                    return@post
                }

                call.respondText("", status = HttpStatusCode.OK)
            }
        }
    }
}

interface SlackSignatureVerifier {
    fun isValid(body: String, timestamp: String, signature: String): Boolean
}

class HmacSlackSignatureVerifier(private val signingSecret: String) : SlackSignatureVerifier {
    override fun isValid(body: String, timestamp: String, signature: String): Boolean {
        val base = "v0:$timestamp:$body"
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(signingSecret.toByteArray(StandardCharsets.UTF_8), "HmacSHA256"))
        val expected = "v0=" + mac.doFinal(base.toByteArray(StandardCharsets.UTF_8)).joinToString("") {
            "%02x".format(it)
        }
        return constantTimeEquals(expected, signature)
    }
}

class SlackEventDedupStore(
    private val clockSeconds: () -> Long = { System.nanoTime() / 1_000_000_000 },
) {
    private val seenEvents = mutableMapOf<String, Long>()

    @Synchronized
    fun isDuplicate(payload: JsonObject): Boolean {
        val key = dedupKeyFromPayload(payload)
        val now = clockSeconds()
        seenEvents.entries.removeIf { (_, seenAt) -> now - seenAt > DEDUP_TTL_SECONDS }

        val seenAt = seenEvents[key]
        if (seenAt != null && now - seenAt <= DEDUP_TTL_SECONDS) {
            return true
        }

        seenEvents[key] = now
        return false
    }
}

fun dedupKeyFromPayload(payload: JsonObject): String {
    payload.stringValue("event_id")?.let { return "event_id:$it" }

    val event = payload["event"]?.jsonObjectOrNull()
    val eventType = event?.stringValue("type").orEmpty()
    val channel = event?.stringValue("channel").orEmpty()
    val ts = event?.stringValue("ts").orEmpty()
    return "fallback:$eventType:$channel:$ts"
}

interface SlackAppMentionScheduler {
    fun schedule(event: JsonObject, mcpClient: ManagedMcpClient?)
}

class CoroutineSlackAppMentionScheduler(
    private val processor: SlackAppMentionProcessor? = null,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default),
) : SlackAppMentionScheduler {
    override fun schedule(event: JsonObject, mcpClient: ManagedMcpClient?) {
        scope.launch {
            processor?.process(event, mcpClient)
        }
    }
}

fun JsonObject.isBotEvent(): Boolean =
    stringValue("subtype") == "bot_message" ||
        !stringValue("bot_id").isNullOrBlank() ||
        stringValue("user").isNullOrBlank()

fun JsonObject.stringValue(name: String): String? =
    this[name]?.jsonPrimitive?.contentOrNull

private val SlackJson = Json {
    ignoreUnknownKeys = true
}

private fun decodeUtf8Strict(bytes: ByteArray): String =
    StandardCharsets.UTF_8.newDecoder()
        .onMalformedInput(CodingErrorAction.REPORT)
        .onUnmappableCharacter(CodingErrorAction.REPORT)
        .decode(java.nio.ByteBuffer.wrap(bytes))
        .toString()

private fun JsonObject?.jsonObjectOrNull(): JsonObject? = this

private fun kotlinx.serialization.json.JsonElement.jsonObjectOrNull(): JsonObject? =
    this as? JsonObject

private fun escapeJsonString(value: String): String =
    value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")

private fun constantTimeEquals(expected: String, actual: String): Boolean {
    if (expected.length != actual.length) {
        return false
    }
    var result = 0
    expected.indices.forEach { index ->
        result = result or (expected[index].code xor actual[index].code)
    }
    return result == 0
}
