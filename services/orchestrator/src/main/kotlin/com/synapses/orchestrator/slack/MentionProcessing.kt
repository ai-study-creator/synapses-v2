package com.synapses.orchestrator.slack

import com.slack.api.Slack
import com.slack.api.methods.SlackApiException
import com.synapses.orchestrator.app.ManagedMcpClient
import com.synapses.orchestrator.llm.ChatCompletionClient
import com.synapses.orchestrator.llm.ToolCall
import com.synapses.orchestrator.llm.getPersonaPrompt
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put

private const val MCP_UNAVAILABLE_MESSAGE = "MCP client is not available. Please check orchestrator startup logs."
private const val UNSUPPORTED_TOOL_PROVIDER_MESSAGE =
    "Configured LLM provider does not support this tool-calling flow yet. Use `LLM_PROVIDER=openai`."
private const val NO_RESPONSE_MESSAGE = "No response was generated."

class SlackPostException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

interface SlackMessageClient {
    suspend fun postMessage(channel: String, threadTs: String, text: String)
}

class SlackJavaSdkMessageClient(
    private val token: String,
) : SlackMessageClient {
    private val methods = Slack.getInstance().methods(token)

    override suspend fun postMessage(channel: String, threadTs: String, text: String) {
        try {
            val response = methods.chatPostMessage { request ->
                request
                    .channel(channel)
                    .threadTs(threadTs)
                    .text(text)
            }
            if (!response.isOk) {
                throw SlackPostException(response.error ?: "Slack API error")
            }
        } catch (error: SlackApiException) {
            throw SlackPostException(error.message ?: "Slack API error", error)
        }
    }
}

class SlackAppMentionProcessor(
    private val llmClient: ChatCompletionClient,
    private val slackClient: SlackMessageClient,
) {
    suspend fun process(event: JsonObject, mcpClient: ManagedMcpClient?) {
        try {
            if (event.isBotEvent()) {
                return
            }

            val channelId = event.stringValue("channel") ?: return
            val threadTs = event.stringValue("thread_ts") ?: event.stringValue("ts") ?: return
            val text = event.stringValue("text").orEmpty()

            if (mcpClient == null) {
                slackClient.postMessage(channelId, threadTs, MCP_UNAVAILABLE_MESSAGE)
                return
            }

            val messages = listOf(
                mapOf("role" to "system", "content" to getPersonaPrompt("devto")),
                mapOf("role" to "user", "content" to text),
            )
            val toolsSchema = mcpToolsToOpenAiSchema(mcpClient)
            val response = llmClient.chatCompletion(
                messages = messages,
                tools = toolsSchema,
                toolChoice = JsonPrimitive("auto"),
            )

            var llmOutput = ""
            var toolResults = emptyList<String>()
            if (llmClient.provider == "openai") {
                val openAiMessage = response.choices.first().message
                if (openAiMessage.toolCalls.isNotEmpty()) {
                    toolResults = executeToolCalls(mcpClient, openAiMessage.toolCalls)
                }
                llmOutput = openAiMessage.content
            } else {
                llmOutput = UNSUPPORTED_TOOL_PROVIDER_MESSAGE
            }

            if (toolResults.isNotEmpty()) {
                llmOutput = toolResults.joinToString("\n")
            }
            if (llmOutput.isEmpty()) {
                llmOutput = NO_RESPONSE_MESSAGE
            }

            slackClient.postMessage(channelId, threadTs, llmOutput)
        } catch (_: SlackPostException) {
            // Source logs Slack API errors and does not rethrow.
        } catch (_: Throwable) {
            // Source logs unexpected mention-processing errors and does not rethrow.
        }
    }
}

suspend fun mcpToolsToOpenAiSchema(mcpClient: ManagedMcpClient): List<JsonObject> =
    mcpClient.listTools().tools.map { tool ->
        buildJsonObject {
            put("type", "function")
            put(
                "function",
                buildJsonObject {
                    put("name", tool.name)
                    put("description", tool.description ?: "")
                    put("parameters", tool.inputSchema.toOpenAiParameters())
                },
            )
        }
    }

suspend fun executeToolCalls(
    mcpClient: ManagedMcpClient,
    toolCalls: List<ToolCall>,
): List<String> {
    val results = mutableListOf<String>()
    toolCalls.forEach { toolCall ->
        val functionName = toolCall.function.name
        val functionArgs = try {
            parseToolArguments(toolCall.function.arguments)
        } catch (_: SerializationException) {
            return@forEach
        } catch (_: IllegalArgumentException) {
            return@forEach
        }
        val toolResult = mcpClient.callTool(functionName, functionArgs)
        results += "Executed `$functionName` successfully: $toolResult"
    }
    return results
}

fun parseToolArguments(arguments: String): Map<String, Any?> {
    val raw = arguments.ifBlank { "{}" }
    val parsed = MentionJson.parseToJsonElement(raw)
    val jsonObject = parsed as? JsonObject ?: return emptyMap()
    return jsonObject.mapValues { (_, value) -> value.toKotlinValue() }
}

private fun ToolSchema.toOpenAiParameters(): JsonObject =
    buildJsonObject {
        put("type", "object")
        put("properties", properties ?: JsonObject(emptyMap()))
        val requiredValues = required.orEmpty()
        if (requiredValues.isNotEmpty()) {
            put("required", JsonArray(requiredValues.map(::JsonPrimitive)))
        }
        val defsValue = defs
        if (defsValue != null && defsValue.isNotEmpty()) {
            put("\$defs", defsValue)
        }
    }

private fun JsonElement.toKotlinValue(): Any? =
    when (this) {
        JsonNull -> null
        is JsonObject -> mapValues { (_, value) -> value.toKotlinValue() }
        is JsonArray -> map { item -> item.toKotlinValue() }
        is JsonPrimitive -> {
            if (isString) {
                content
            } else {
                booleanOrNull ?: longOrNull ?: doubleOrNull ?: contentOrNull
            }
        }
    }

private val MentionJson = Json {
    ignoreUnknownKeys = true
}
