package com.synapses.orchestrator.slack

import com.synapses.orchestrator.app.ManagedMcpClient
import com.synapses.orchestrator.llm.ChatCompletionClient
import com.synapses.orchestrator.llm.ChatCompletionResult
import com.synapses.orchestrator.llm.ChatMessage
import com.synapses.orchestrator.llm.Choice
import com.synapses.orchestrator.llm.ToolCall
import com.synapses.orchestrator.llm.ToolFunction
import com.synapses.orchestrator.llm.getPersonaPrompt
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.ListToolsResult
import io.modelcontextprotocol.kotlin.sdk.types.Tool
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MentionProcessingTest {
    @Test
    fun `TEST-MENTION-001 ignores bot self events`() = runTest {
        val slack = RecordingSlackMessageClient()
        val processor = SlackAppMentionProcessor(
            llmClient = RecordingChatCompletionClient(),
            slackClient = slack,
        )

        processor.process(
            event = mentionEvent(subtype = "bot_message"),
            mcpClient = RecordingMentionMcpClient(),
        )

        assertEquals(emptyList<PostedSlackMessage>(), slack.posts)
    }

    @Test
    fun `TEST-MENTION-002 uses channel and thread timestamp when MCP is unavailable`() = runTest {
        val slack = RecordingSlackMessageClient()
        val processor = SlackAppMentionProcessor(
            llmClient = RecordingChatCompletionClient(),
            slackClient = slack,
        )

        processor.process(
            event = mentionEvent(channel = "C1", ts = "100.1", threadTs = "99.9"),
            mcpClient = null,
        )

        assertEquals(
            listOf(
                PostedSlackMessage(
                    channel = "C1",
                    threadTs = "99.9",
                    text = "MCP client is not available. Please check orchestrator startup logs.",
                ),
            ),
            slack.posts,
        )
    }

    @Test
    fun `TEST-MENTION-003 falls back to event timestamp when thread timestamp is missing`() = runTest {
        val slack = RecordingSlackMessageClient()
        val processor = SlackAppMentionProcessor(
            llmClient = RecordingChatCompletionClient(),
            slackClient = slack,
        )

        processor.process(
            event = mentionEvent(channel = "C1", ts = "100.1", threadTs = null),
            mcpClient = null,
        )

        assertEquals("100.1", slack.posts.single().threadTs)
    }

    @Test
    fun `TEST-MENTION-004 builds devto persona prompt and sends MCP tools to LLM`() = runTest {
        val llm = RecordingChatCompletionClient(
            result = chatResult(content = "drafted"),
        )
        val slack = RecordingSlackMessageClient()
        val mcp = RecordingMentionMcpClient(
            tools = listOf(
                Tool(
                    name = "create_dev_post",
                    description = "Creates a new draft post on Dev.to.",
                    inputSchema = ToolSchema(
                        properties = buildJsonObject {
                            put("title", buildJsonObject { put("type", "string") })
                        },
                        required = listOf("title"),
                    ),
                ),
            ),
        )

        SlackAppMentionProcessor(llm, slack).process(
            event = mentionEvent(text = "write about ktor"),
            mcpClient = mcp,
        )

        assertEquals(getPersonaPrompt("devto"), llm.messages.single()[0]["content"])
        assertEquals(mapOf("role" to "user", "content" to "write about ktor"), llm.messages.single()[1])
        assertEquals("auto", llm.toolChoices.single()?.jsonPrimitive?.content)

        val function = llm.tools.single().single()["function"]!!.jsonObject
        assertEquals("create_dev_post", function["name"]?.jsonPrimitive?.content)
        assertEquals("Creates a new draft post on Dev.to.", function["description"]?.jsonPrimitive?.content)
        val parameters = function["parameters"]!!.jsonObject
        assertEquals("object", parameters["type"]?.jsonPrimitive?.content)
        assertEquals("title", parameters["required"]?.jsonArray?.single()?.jsonPrimitive?.content)
    }

    @Test
    fun `TEST-MENTION-005 executes OpenAI tool calls and tool results replace LLM text`() = runTest {
        val toolCall = ToolCall(
            id = "call-1",
            type = "function",
            function = ToolFunction(
                name = "create_dev_post",
                arguments = """{"title":"T","tags":["kotlin"],"published":false}""",
            ),
        )
        val llm = RecordingChatCompletionClient(result = chatResult(content = "fallback text", toolCalls = listOf(toolCall)))
        val slack = RecordingSlackMessageClient()
        val mcp = RecordingMentionMcpClient()

        SlackAppMentionProcessor(llm, slack).process(mentionEvent(), mcp)

        assertEquals("create_dev_post", mcp.toolCalls.single().name)
        assertEquals("T", mcp.toolCalls.single().arguments["title"])
        assertEquals(listOf("kotlin"), mcp.toolCalls.single().arguments["tags"])
        assertEquals(false, mcp.toolCalls.single().arguments["published"])
        assertTrue(slack.posts.single().text.startsWith("Executed `create_dev_post` successfully:"))
        assertTrue(slack.posts.single().text.contains("CallToolResult"))
    }

    @Test
    fun `TEST-MENTION-006 non OpenAI providers return source tool calling warning`() = runTest {
        val slack = RecordingSlackMessageClient()
        val llm = RecordingChatCompletionClient(provider = "github_models", result = chatResult(content = "ignored"))

        SlackAppMentionProcessor(llm, slack).process(mentionEvent(), RecordingMentionMcpClient())

        assertEquals(
            "Configured LLM provider does not support this tool-calling flow yet. Use `LLM_PROVIDER=openai`.",
            slack.posts.single().text,
        )
    }

    @Test
    fun `TEST-MENTION-007 empty final output becomes no response message`() = runTest {
        val slack = RecordingSlackMessageClient()

        SlackAppMentionProcessor(
            llmClient = RecordingChatCompletionClient(result = chatResult(content = "")),
            slackClient = slack,
        ).process(mentionEvent(), RecordingMentionMcpClient())

        assertEquals("No response was generated.", slack.posts.single().text)
    }

    @Test
    fun `TEST-MCP-CLIENT-002 skips tool calls with invalid JSON arguments`() = runTest {
        val invalidCall = ToolCall(
            id = "call-1",
            type = "function",
            function = ToolFunction(name = "create_dev_post", arguments = "{not-json"),
        )
        val mcp = RecordingMentionMcpClient()

        val results = executeToolCalls(mcp, listOf(invalidCall))

        assertEquals(emptyList<String>(), results)
        assertEquals(emptyList<RecordedToolCall>(), mcp.toolCalls)
    }

    @Test
    fun `TEST-MENTION-008 Slack and unexpected processing errors are not rethrown`() {
        assertDoesNotThrow {
            runTest {
                SlackAppMentionProcessor(
                    llmClient = RecordingChatCompletionClient(),
                    slackClient = RecordingSlackMessageClient(error = SlackPostException("slack failed")),
                ).process(mentionEvent(), null)
            }
        }

        assertDoesNotThrow {
            runTest {
                SlackAppMentionProcessor(
                    llmClient = RecordingChatCompletionClient(error = IllegalStateException("llm failed")),
                    slackClient = RecordingSlackMessageClient(),
                ).process(mentionEvent(), RecordingMentionMcpClient())
            }
        }
    }

    private fun mentionEvent(
        channel: String = "C1",
        ts: String = "100.1",
        threadTs: String? = "100.1",
        text: String = "hello",
        subtype: String? = null,
    ): JsonObject =
        buildJsonObject {
            put("type", "app_mention")
            put("user", "U1")
            put("channel", channel)
            put("ts", ts)
            if (threadTs != null) {
                put("thread_ts", threadTs)
            }
            put("text", text)
            if (subtype != null) {
                put("subtype", subtype)
            }
        }

    private fun chatResult(
        content: String,
        toolCalls: List<ToolCall> = emptyList(),
    ): ChatCompletionResult =
        ChatCompletionResult(
            choices = listOf(
                Choice(
                    ChatMessage(
                        role = "assistant",
                        content = content,
                        toolCalls = toolCalls,
                    ),
                ),
            ),
        )
}

private data class PostedSlackMessage(
    val channel: String,
    val threadTs: String,
    val text: String,
)

private class RecordingSlackMessageClient(
    private val error: Throwable? = null,
) : SlackMessageClient {
    val posts = mutableListOf<PostedSlackMessage>()

    override suspend fun postMessage(channel: String, threadTs: String, text: String) {
        error?.let { throw it }
        posts += PostedSlackMessage(channel, threadTs, text)
    }
}

private class RecordingChatCompletionClient(
    override val provider: String = "openai",
    private val result: ChatCompletionResult = ChatCompletionResult(
        choices = listOf(Choice(ChatMessage(role = "assistant", content = "ok", toolCalls = emptyList()))),
    ),
    private val error: Throwable? = null,
) : ChatCompletionClient {
    val messages = mutableListOf<List<Map<String, String>>>()
    val tools = mutableListOf<List<JsonObject>>()
    val toolChoices = mutableListOf<JsonElement?>()

    override suspend fun chatCompletion(
        messages: List<Map<String, String>>,
        tools: List<JsonObject>?,
        toolChoice: JsonElement?,
        model: String?,
        temperature: Double,
        timeoutSeconds: Int,
    ): ChatCompletionResult {
        error?.let { throw it }
        this.messages += messages
        this.tools += tools.orEmpty()
        this.toolChoices += toolChoice
        return result
    }
}

private data class RecordedToolCall(
    val name: String,
    val arguments: Map<String, Any?>,
)

private class RecordingMentionMcpClient(
    private val tools: List<Tool> = emptyList(),
) : ManagedMcpClient {
    val toolCalls = mutableListOf<RecordedToolCall>()

    override suspend fun listTools(): ListToolsResult =
        ListToolsResult(tools = tools)

    override suspend fun callTool(name: String, arguments: Map<String, Any?>): CallToolResult {
        toolCalls += RecordedToolCall(name, arguments)
        return CallToolResult(
            content = emptyList(),
            isError = false,
            structuredContent = buildJsonObject { put("ok", true) },
            meta = JsonObject(emptyMap()),
        )
    }

    override suspend fun close() = Unit
}
