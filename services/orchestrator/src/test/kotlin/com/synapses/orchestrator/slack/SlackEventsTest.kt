package com.synapses.orchestrator.slack

import com.synapses.orchestrator.app.McpServerCommand
import com.synapses.orchestrator.app.OrchestratorRuntime
import com.synapses.orchestrator.app.OrchestratorRuntimeKey
import com.synapses.orchestrator.app.RecordingManagedMcpClient
import com.synapses.orchestrator.app.RecordingMcpClientFactory
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SlackEventsTest {
    @Test
    fun `TEST-SLACK-001 rejects invalid UTF-8 and invalid signatures`() = testApplication {
        application {
            installSlackTestRoutes(signatureVerifier = FixedSignatureVerifier(valid = false))
        }

        val invalidEncoding = client.post("/slack/events") {
            setBody(byteArrayOf(0xC3.toByte()))
        }
        assertEquals(HttpStatusCode.BadRequest, invalidEncoding.status)
        assertEquals("Invalid request encoding", invalidEncoding.bodyAsText())

        val invalidSignature = client.postJson("""{"type":"event_callback"}""")
        assertEquals(HttpStatusCode.Forbidden, invalidSignature.status)
        assertEquals("Invalid Slack signature", invalidSignature.bodyAsText())
    }

    @Test
    fun `TEST-SLACK-002 Slack retry returns 200 without processing`() = testApplication {
        val scheduler = RecordingMentionScheduler()
        application {
            installSlackTestRoutes(processor = scheduler)
        }

        val response = client.postJson(
            body = """{"event_id":"E1","event":{"type":"app_mention","user":"U1","channel":"C1","ts":"1"}}""",
            extraHeaders = mapOf("x-slack-retry-num" to "1"),
        )

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(0, scheduler.events.size)
    }

    @Test
    fun `TEST-SLACK-003 invalid JSON returns 400`() = testApplication {
        application {
            installSlackTestRoutes()
        }

        val response = client.postJson("{not-json")

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals("Invalid JSON payload", response.bodyAsText())
    }

    @Test
    fun `TEST-SLACK-004 URL verification returns challenge or missing challenge error`() = testApplication {
        application {
            installSlackTestRoutes()
        }

        val valid = client.postJson("""{"type":"url_verification","challenge":"abc"}""")
        assertEquals(HttpStatusCode.OK, valid.status)
        assertEquals("""{"challenge":"abc"}""", valid.bodyAsText())

        val missing = client.postJson("""{"type":"url_verification"}""")
        assertEquals(HttpStatusCode.BadRequest, missing.status)
        assertEquals("Missing challenge", missing.bodyAsText())
    }

    @Test
    fun `TEST-SLACK-005 dedup keys and TTL behavior are preserved`() {
        assertEquals(
            "event_id:E1",
            dedupKeyFromPayload(
                buildJsonObject {
                    put("event_id", "E1")
                    put("event", buildJsonObject { put("type", "app_mention") })
                },
            ),
        )
        assertEquals(
            "fallback:app_mention:C1:123.45",
            dedupKeyFromPayload(
                buildJsonObject {
                    put(
                        "event",
                        buildJsonObject {
                            put("type", "app_mention")
                            put("channel", "C1")
                            put("ts", "123.45")
                        },
                    )
                },
            ),
        )

        var now = 100L
        val store = SlackEventDedupStore(clockSeconds = { now })
        val payload = buildJsonObject { put("event_id", "E1") }

        assertFalse(store.isDuplicate(payload))
        assertTrue(store.isDuplicate(payload))
        now = 161L
        assertFalse(store.isDuplicate(payload))
    }

    @Test
    fun `TEST-SLACK-006 no event and non app mention payloads return 200`() = testApplication {
        application {
            installSlackTestRoutes()
        }

        assertEquals(HttpStatusCode.OK, client.postJson("""{"event_id":"E1"}""").status)
        assertEquals(
            HttpStatusCode.OK,
            client.postJson("""{"event_id":"E2","event":{"type":"message","user":"U1","channel":"C1","ts":"1"}}""").status,
        )
    }

    @Test
    fun `TEST-SLACK-007 bot self app mentions skip processing`() = testApplication {
        val scheduler = RecordingMentionScheduler()
        application {
            installSlackTestRoutes(processor = scheduler)
        }

        val botSubtype = client.postJson(
            """{"event_id":"E1","event":{"type":"app_mention","subtype":"bot_message","user":"U1","channel":"C1","ts":"1"}}""",
        )
        val botId = client.postJson(
            """{"event_id":"E2","event":{"type":"app_mention","bot_id":"B1","user":"U1","channel":"C1","ts":"1"}}""",
        )
        val noUser = client.postJson(
            """{"event_id":"E3","event":{"type":"app_mention","channel":"C1","ts":"1"}}""",
        )

        assertEquals(HttpStatusCode.OK, botSubtype.status)
        assertEquals(HttpStatusCode.OK, botId.status)
        assertEquals(HttpStatusCode.OK, noUser.status)
        assertEquals(0, scheduler.events.size)
    }

    @Test
    fun `TEST-SLACK-008 user app mentions enqueue processing and return immediately`() = testApplication {
        val scheduler = RecordingMentionScheduler()
        val mcpClient = RecordingManagedMcpClient()
        application {
            installSlackTestRoutes(
                processor = scheduler,
                mcpClient = mcpClient,
            )
        }

        val response = client.postJson(
            """{"event_id":"E1","event":{"type":"app_mention","user":"U1","channel":"C1","ts":"1"}}""",
        )

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(1, scheduler.events.size)
        assertEquals("U1", scheduler.events.single().stringValue("user"))
        assertEquals(mcpClient, scheduler.mcpClients.single())
    }

    private fun io.ktor.server.application.Application.installSlackTestRoutes(
        signatureVerifier: SlackSignatureVerifier = FixedSignatureVerifier(valid = true),
        processor: SlackAppMentionScheduler = RecordingMentionScheduler(),
        mcpClient: RecordingManagedMcpClient = RecordingManagedMcpClient(),
    ) {
        val runtime = OrchestratorRuntime(
            mcpClientFactory = RecordingMcpClientFactory(client = mcpClient),
            mcpCommand = McpServerCommand("mcp-devto", emptyList(), emptyMap()),
        )
        kotlinx.coroutines.runBlocking { runtime.start() }
        attributes.put(OrchestratorRuntimeKey, runtime)
        slackEventsRoutes(
            signingSecret = "secret",
            signatureVerifier = signatureVerifier,
            processor = processor,
        )
    }

    private suspend fun io.ktor.client.HttpClient.postJson(
        body: String,
        extraHeaders: Map<String, String> = emptyMap(),
    ) = post("/slack/events") {
        header(HttpHeaders.ContentType, ContentType.Application.Json)
        header("x-slack-request-timestamp", "123")
        header("x-slack-signature", "v0=test")
        extraHeaders.forEach { (name, value) -> header(name, value) }
        setBody(body)
    }
}

private class FixedSignatureVerifier(private val valid: Boolean) : SlackSignatureVerifier {
    override fun isValid(body: String, timestamp: String, signature: String): Boolean = valid
}

private class RecordingMentionScheduler : SlackAppMentionScheduler {
    val events = mutableListOf<JsonObject>()
    val mcpClients = mutableListOf<Any?>()

    override fun schedule(event: JsonObject, mcpClient: com.synapses.orchestrator.app.ManagedMcpClient?) {
        events += event
        mcpClients += mcpClient
    }
}
