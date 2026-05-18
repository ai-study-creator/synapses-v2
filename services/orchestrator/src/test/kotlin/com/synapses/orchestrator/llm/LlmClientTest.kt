package com.synapses.orchestrator.llm

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpResponseData
import io.ktor.client.request.HttpRequestData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LlmClientTest {
    @Test
    fun `TEST-LLM-001 normalizes providers and resolves API key fallbacks`() {
        assertEquals("openai", LlmClient("", "configured").provider)
        assertEquals("github_models", LlmClient("github", "", mapOf("GITHUB_TOKEN" to "gh-token")).provider)
        assertEquals("github_models", LlmClient("github-models", "", mapOf("GH_TOKEN" to "gh-token")).provider)

        val openAi = LlmClient("openai", "", mapOf("OPENAI_API_KEY" to "env-openai")).requestConfig()
        assertEquals("Bearer env-openai", openAi.headers[HttpHeaders.Authorization])

        val github = LlmClient(
            provider = "github_models",
            apiKey = "",
            environment = mapOf(
                "GITHUB_MODELS_TOKEN" to "models-token",
                "GITHUB_TOKEN" to "github-token",
                "GH_TOKEN" to "gh-token",
            ),
        ).requestConfig()
        assertEquals("Bearer models-token", github.headers[HttpHeaders.Authorization])

        val unsupported = assertThrows(IllegalArgumentException::class.java) {
            LlmClient("anthropic", "key")
        }
        assertEquals("Unsupported LLM provider. Use 'openai' or 'github_models'.", unsupported.message)

        val missing = assertThrows(IllegalArgumentException::class.java) {
            LlmClient("openai", "", emptyMap())
        }
        assertEquals("Missing API key for configured LLM provider.", missing.message)
    }

    @Test
    fun `TEST-LLM-002 selects default models and request configs`() {
        assertEquals(
            "global-model",
            LlmClient("openai", "key", mapOf("AI_MODEL" to "global-model")).defaultModel(),
        )
        assertEquals(
            "gpt-4.1-mini",
            LlmClient("openai", "key", emptyMap()).defaultModel(),
        )
        assertEquals(
            "github-model",
            LlmClient("github_models", "key", mapOf("GITHUB_MODELS_MODEL" to "github-model")).defaultModel(),
        )
        assertEquals(
            "openai/gpt-4.1",
            LlmClient("github_models", "key", emptyMap()).defaultModel(),
        )

        val openAi = LlmClient("openai", "openai-key").requestConfig()
        assertEquals(OPENAI_URL, openAi.url)
        assertEquals("Bearer openai-key", openAi.headers[HttpHeaders.Authorization])
        assertEquals(ContentType.Application.Json.toString(), openAi.headers[HttpHeaders.ContentType])

        val github = LlmClient(
            provider = "github_models",
            apiKey = "github-key",
            environment = mapOf(
                "GITHUB_MODELS_URL" to "https://example.test/chat",
                "GITHUB_API_VERSION" to "2099-01-01",
            ),
        ).requestConfig()
        assertEquals("https://example.test/chat", github.url)
        assertEquals("Bearer github-key", github.headers[HttpHeaders.Authorization])
        assertEquals(ContentType.Application.Json.toString(), github.headers[HttpHeaders.ContentType])
        assertEquals("application/vnd.github+json", github.headers[HttpHeaders.Accept])
        assertEquals("2099-01-01", github.headers["X-GitHub-Api-Version"])
    }

    @Test
    fun `TEST-LLM-003 validates messages and sends chat payload`() = runTest {
        var capturedBody: JsonObject? = null
        val client = LlmClient(
            provider = "openai",
            apiKey = "key",
            httpClient = mockHttpClient { request ->
                capturedBody = defaultJson.parseToJsonElement(requestBodyText(request)).jsonObject
                respondJson("""{"choices":[{"message":{"role":"assistant","content":"done"}}]}""")
            },
        )

        val result = client.chatCompletion(
            messages = listOf(mapOf("role" to "user", "content" to "Create a post")),
            tools = listOf(
                buildJsonObject {
                    put("type", "function")
                    put("function", buildJsonObject { put("name", "create_devto_post") })
                },
            ),
            toolChoice = JsonPrimitive("auto"),
            model = "test-model",
            temperature = 0.4,
        )

        assertEquals("done", result.choices.single().message.content)
        val body = checkNotNull(capturedBody)
        assertEquals("test-model", body["model"]?.jsonPrimitive?.content)
        assertEquals("user", body["messages"]?.jsonArray?.single()?.jsonObject?.get("role")?.jsonPrimitive?.content)
        assertEquals("function", body["tools"]?.jsonArray?.single()?.jsonObject?.get("type")?.jsonPrimitive?.content)
        assertEquals("auto", body["tool_choice"]?.jsonPrimitive?.content)

        assertThrows(IllegalArgumentException::class.java) {
            client.validateMessages(emptyList())
        }
        assertThrows(IllegalArgumentException::class.java) {
            client.validateMessages(listOf(mapOf("role" to "bad", "content" to "hello")))
        }
        assertThrows(IllegalArgumentException::class.java) {
            client.validateMessages(listOf(mapOf("role" to "user", "content" to "")))
        }
        assertEquals(
            listOf(mapOf("role" to "tool", "content" to "")),
            client.validateMessages(listOf(mapOf("role" to "tool", "content" to ""))),
        )
    }

    @Test
    fun `TEST-LLM-004 retries transient responses and reports provider errors`() = runTest {
        var attempts = 0
        val sleeps = mutableListOf<Long>()
        val retryingClient = LlmClient(
            provider = "openai",
            apiKey = "key",
            httpClient = mockHttpClient {
                attempts += 1
                if (attempts < 3) {
                    respondJson("""{"error":"busy"}""", HttpStatusCode.TooManyRequests)
                } else {
                    respondJson("""{"choices":[{"message":{"role":"assistant","content":"ok"}}]}""")
                }
            },
            sleeper = { sleeps += it },
        )

        assertEquals("ok", retryingClient.chatCompletion(validMessages()).choices.single().message.content)
        assertEquals(3, attempts)
        assertEquals(listOf(500L, 1000L), sleeps)

        val failingClient = LlmClient(
            provider = "openai",
            apiKey = "key",
            httpClient = mockHttpClient {
                respond(" line one\nline two ", HttpStatusCode.BadRequest)
            },
        )
        val apiError = expectLlmException {
            failingClient.chatCompletion(validMessages())
        }
        assertEquals("LLM API error 400: line one line two", apiError.message)

        val invalidJsonClient = LlmClient(
            provider = "openai",
            apiKey = "key",
            httpClient = mockHttpClient {
                respondJson("not json")
            },
        )
        val jsonError = expectLlmException {
            invalidJsonClient.chatCompletion(validMessages())
        }
        assertEquals("LLM provider returned invalid JSON.", jsonError.message)
    }

    @Test
    fun `TEST-LLM-005 parses text and tool call responses`() {
        val client = LlmClient("openai", "key")
        val parsed = client.parseResponse(
            defaultJson.parseToJsonElement(
                """
                {
                  "choices": [
                    {
                      "message": {
                        "role": "assistant",
                        "content": [{"text":"first"}, {"text":"second"}],
                        "tool_calls": [
                          {
                            "id": "call-1",
                            "type": "function",
                            "function": {
                              "name": "create_devto_post",
                              "arguments": {"title":"T"}
                            }
                          },
                          {
                            "function": {
                              "name": "plain_args",
                              "arguments": "{\"ok\":true}"
                            }
                          },
                          {
                            "function": {
                              "name": "fallback_args",
                              "arguments": 12
                            }
                          }
                        ]
                      }
                    }
                  ]
                }
                """.trimIndent(),
            ).jsonObject,
        )

        val message = parsed.choices.single().message
        assertEquals("first\nsecond", message.content)
        assertEquals("call-1", message.toolCalls[0].id)
        assertEquals("create_devto_post", message.toolCalls[0].function.name)
        assertEquals("""{"title":"T"}""", message.toolCalls[0].function.arguments)
        assertEquals("""{"ok":true}""", message.toolCalls[1].function.arguments)
        assertEquals("12", message.toolCalls[2].function.arguments)

        assertThrows(LlmException::class.java) {
            client.parseResponse(defaultJson.parseToJsonElement("""{"choices":[]}""").jsonObject)
        }
        assertThrows(LlmException::class.java) {
            client.parseResponse(defaultJson.parseToJsonElement("""{"choices":[{"message":{"content":""}}]}""").jsonObject)
        }
        assertTrue(sanitizeBody("x".repeat(1005)).endsWith("..."))
    }

    private fun validMessages(): List<Map<String, String>> =
        listOf(mapOf("role" to "user", "content" to "hello"))

    private fun mockHttpClient(handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData): HttpClient =
        HttpClient(MockEngine { request -> handler(request) }) {
            install(ContentNegotiation) {
                json(defaultJson)
            }
            expectSuccess = false
        }

    private fun MockRequestHandleScope.respondJson(
        content: String,
        status: HttpStatusCode = HttpStatusCode.OK,
    ): HttpResponseData =
        respond(
            content = content,
            status = status,
            headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
        )

    private fun requestBodyText(request: HttpRequestData): String {
        val body = request.body as OutgoingContent.ByteArrayContent
        return body.bytes().decodeToString()
    }

    private suspend fun expectLlmException(block: suspend () -> Unit): LlmException {
        try {
            block()
        } catch (error: LlmException) {
            return error
        }

        throw AssertionError("Expected LlmException")
    }
}
