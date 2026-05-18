package com.synapses.orchestrator.llm

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

const val OPENAI_URL = "https://api.openai.com/v1/chat/completions"
const val GITHUB_MODELS_URL = "https://models.github.ai/inference/chat/completions"

class LlmException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

data class ToolFunction(
    val name: String,
    val arguments: String,
)

data class ToolCall(
    val id: String,
    val type: String,
    val function: ToolFunction,
)

data class ChatMessage(
    val role: String,
    val content: String,
    val toolCalls: List<ToolCall>,
)

data class Choice(
    val message: ChatMessage,
)

data class ChatCompletionResult(
    val choices: List<Choice>,
)

data class RequestConfig(
    val url: String,
    val headers: Map<String, String>,
)

interface ChatCompletionClient {
    val provider: String

    suspend fun chatCompletion(
        messages: List<Map<String, String>>,
        tools: List<JsonObject>? = null,
        toolChoice: JsonElement? = null,
        model: String? = null,
        temperature: Double = 0.2,
        timeoutSeconds: Int = 60,
    ): ChatCompletionResult
}

class LlmClient(
    provider: String,
    apiKey: String,
    private val environment: Map<String, String> = System.getenv(),
    private val httpClient: HttpClient = defaultHttpClient(),
    private val json: Json = defaultJson,
    private val sleeper: suspend (Long) -> Unit = { delay(it) },
) : ChatCompletionClient {
    override val provider: String = normalizeProvider(provider)
    private val apiKey: String = resolveApiKey(this.provider, apiKey)

    init {
        if (this.apiKey.isBlank()) {
            throw IllegalArgumentException("Missing API key for configured LLM provider.")
        }
    }

    override suspend fun chatCompletion(
        messages: List<Map<String, String>>,
        tools: List<JsonObject>?,
        toolChoice: JsonElement?,
        model: String?,
        temperature: Double,
        timeoutSeconds: Int,
    ): ChatCompletionResult {
        val payloadMessages = validateMessages(messages)
        val modelName = model ?: defaultModel()
        val payload = buildJsonObject {
            put("model", modelName)
            put("messages", mapMessages(payloadMessages))
            put("temperature", temperature)
            if (!tools.isNullOrEmpty()) {
                put("tools", JsonArray(tools))
            }
            if (toolChoice != null) {
                put("tool_choice", toolChoice)
            }
        }

        val requestConfig = requestConfig()
        val data = postWithRetries(
            url = requestConfig.url,
            headers = requestConfig.headers,
            payload = payload,
            timeoutSeconds = timeoutSeconds,
        )
        return parseResponse(data)
    }

    fun defaultModel(): String {
        val explicit = environment["AI_MODEL"].orEmpty().trim()
        if (explicit.isNotEmpty()) {
            return explicit
        }

        if (provider == "openai") {
            return environment["OPENAI_MODEL"].orEmpty().trim().ifEmpty { "gpt-4.1-mini" }
        }

        return environment["GITHUB_MODELS_MODEL"].orEmpty().trim().ifEmpty { "openai/gpt-4.1" }
    }

    fun requestConfig(): RequestConfig {
        if (provider == "openai") {
            return RequestConfig(
                url = OPENAI_URL,
                headers = mapOf(
                    HttpHeaders.Authorization to "Bearer $apiKey",
                    HttpHeaders.ContentType to ContentType.Application.Json.toString(),
                ),
            )
        }

        return RequestConfig(
            url = environment["GITHUB_MODELS_URL"].orEmpty().trim().ifEmpty { GITHUB_MODELS_URL },
            headers = mapOf(
                HttpHeaders.Authorization to "Bearer $apiKey",
                HttpHeaders.ContentType to ContentType.Application.Json.toString(),
                HttpHeaders.Accept to "application/vnd.github+json",
                "X-GitHub-Api-Version" to (environment["GITHUB_API_VERSION"] ?: "2022-11-28"),
            ),
        )
    }

    fun validateMessages(messages: List<Map<String, String>>): List<Map<String, String>> {
        if (messages.isEmpty()) {
            throw IllegalArgumentException("messages must be a non-empty list")
        }

        return messages.mapIndexed { index, item ->
            val role = item["role"].orEmpty().trim()
            val content = item["content"].orEmpty().trim()
            if (role !in VALID_MESSAGE_ROLES) {
                throw IllegalArgumentException("messages[$index].role is invalid: '$role'")
            }
            if (content.isEmpty() && role != "tool") {
                throw IllegalArgumentException("messages[$index].content must be non-empty")
            }
            mapOf("role" to role, "content" to content)
        }
    }

    fun parseResponse(data: JsonObject): ChatCompletionResult {
        val choices = data["choices"] as? JsonArray
        if (choices == null || choices.isEmpty()) {
            throw LlmException("LLM response missing choices.")
        }

        val firstChoice = choices.firstOrNull() as? JsonObject ?: JsonObject(emptyMap())
        val message = firstChoice["message"] as? JsonObject
            ?: throw LlmException("LLM response missing message.")

        val content = extractTextContent(message["content"])
        val toolCalls = extractToolCalls(message["tool_calls"])

        if (content.isEmpty() && toolCalls.isEmpty()) {
            throw LlmException("LLM response did not include content or tool calls.")
        }

        return ChatCompletionResult(
            choices = listOf(
                Choice(
                    message = ChatMessage(
                        role = message["role"]?.jsonPrimitive?.contentOrNull ?: "assistant",
                        content = content,
                        toolCalls = toolCalls,
                    ),
                ),
            ),
        )
    }

    suspend fun postWithRetries(
        url: String,
        headers: Map<String, String>,
        payload: JsonObject,
        timeoutSeconds: Int,
        maxAttempts: Int = 3,
    ): JsonObject {
        val transientStatuses = setOf(408, 409, 429, 500, 502, 503, 504)
        var lastError: Throwable? = null

        for (attempt in 1..maxAttempts) {
            val response = try {
                post(url, headers, payload, timeoutSeconds)
            } catch (error: Throwable) {
                lastError = error
                if (attempt == maxAttempts) {
                    break
                }
                sleeper(500L * attempt)
                continue
            }

            if (response.status.isSuccess()) {
                return try {
                    response.body()
                } catch (error: Throwable) {
                    throw LlmException("LLM provider returned invalid JSON.", error)
                }
            }

            if (response.status.value in transientStatuses && attempt < maxAttempts) {
                sleeper(500L * attempt)
                continue
            }

            val sanitized = sanitizeBody(response.bodyAsText())
            throw LlmException("LLM API error ${response.status.value}: $sanitized")
        }

        throw LlmException("LLM request failed after retries: $lastError", lastError)
    }

    private suspend fun post(
        url: String,
        headers: Map<String, String>,
        payload: JsonObject,
        timeoutSeconds: Int,
    ): HttpResponse {
        return try {
            httpClient.post(url) {
                headers.forEach { (name, value) -> header(name, value) }
                header("X-Timeout-Seconds", timeoutSeconds.toString())
                setBody(payload)
            }
        } catch (error: ClientRequestException) {
            error.response
        } catch (error: ServerResponseException) {
            error.response
        }
    }

    private fun resolveApiKey(provider: String, configuredApiKey: String): String {
        val configured = configuredApiKey.trim()
        if (configured.isNotEmpty()) {
            return configured
        }

        if (provider == "openai") {
            return environment["OPENAI_API_KEY"].orEmpty().trim()
        }

        return listOf("GITHUB_MODELS_TOKEN", "GITHUB_TOKEN", "GH_TOKEN")
            .firstNotNullOfOrNull { environment[it]?.trim()?.takeIf(String::isNotEmpty) }
            .orEmpty()
    }

    companion object {
        private val VALID_MESSAGE_ROLES = setOf("system", "user", "assistant", "tool")

        fun normalizeProvider(provider: String): String {
            val normalized = provider.trim().lowercase().ifEmpty { "openai" }
            return when (normalized) {
                "github", "github-models" -> "github_models"
                "openai", "github_models" -> normalized
                else -> throw IllegalArgumentException("Unsupported LLM provider. Use 'openai' or 'github_models'.")
            }
        }
    }
}

fun extractTextContent(content: JsonElement?): String {
    return when (content) {
        is JsonPrimitive -> content.contentOrNull.orEmpty().trim()
        is JsonArray -> content.mapNotNull { item ->
            (item as? JsonObject)
                ?.get("text")
                ?.jsonPrimitive
                ?.contentOrNull
                ?.trim()
                ?.takeIf(String::isNotEmpty)
        }.joinToString("\n").trim()
        else -> ""
    }
}

fun extractToolCalls(value: JsonElement?): List<ToolCall> {
    val rawCalls = value as? JsonArray ?: return emptyList()
    return rawCalls.mapNotNull { raw ->
        val item = raw as? JsonObject ?: return@mapNotNull null
        val rawFunction = item["function"] as? JsonObject ?: return@mapNotNull null
        val name = rawFunction["name"]?.jsonPrimitive?.contentOrNull.orEmpty().trim()
        if (name.isEmpty()) {
            return@mapNotNull null
        }

        val arguments = when (val rawArguments = rawFunction["arguments"]) {
            is JsonObject -> rawArguments.toString()
            is JsonPrimitive -> rawArguments.contentOrNull ?: "{}"
            else -> "{}"
        }

        ToolCall(
            id = item["id"]?.jsonPrimitive?.contentOrNull ?: "",
            type = item["type"]?.jsonPrimitive?.contentOrNull ?: "function",
            function = ToolFunction(name = name, arguments = arguments),
        )
    }
}

fun sanitizeBody(text: String, limit: Int = 1000): String {
    val cleaned = text.trim().lines().joinToString(" ") { it.trim() }
    return if (cleaned.length > limit) {
        "${cleaned.take(limit)}..."
    } else {
        cleaned
    }
}

private fun mapMessages(messages: List<Map<String, String>>): JsonArray =
    buildJsonArray {
        messages.forEach { message ->
            add(
                buildJsonObject {
                    put("role", message["role"].orEmpty())
                    put("content", message["content"].orEmpty())
                },
            )
        }
    }

val defaultJson: Json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    explicitNulls = false
}

fun defaultHttpClient(): HttpClient =
    HttpClient(CIO) {
        install(ContentNegotiation) {
            json(defaultJson)
        }
        expectSuccess = false
    }
