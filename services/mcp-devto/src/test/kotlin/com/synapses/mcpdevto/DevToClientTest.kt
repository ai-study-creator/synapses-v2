package com.synapses.mcpdevto

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.http.content.OutgoingContent
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test

class DevToClientTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `TEST-MCP-DEVTO-005 sends DevTo draft article payload`() = runTest {
        lateinit var capturedBody: String
        lateinit var capturedApiKey: String

        val client = DevToClient(
            apiKey = "devto-secret",
            httpClient = mockHttpClient {
                capturedApiKey = it.headers["api-key"].orEmpty()
                capturedBody = (it.body as OutgoingContent.ByteArrayContent).bytes().decodeToString()
                respond(
                    content = """{"id":123,"url":"https://dev.to/example/post"}""",
                    status = HttpStatusCode.Created,
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
            },
            articlesUrl = "https://dev.to/api/articles",
        )

        client.createDraftPost(
            title = "A Kotlin migration",
            bodyMarkdown = "# Hello",
            tags = listOf("kotlin", "mcp"),
        )

        val payload = json.decodeFromString<DevToCreateArticleRequest>(capturedBody)
        assertEquals("devto-secret", capturedApiKey)
        assertEquals("A Kotlin migration", payload.article.title)
        assertEquals("# Hello", payload.article.bodyMarkdown)
        assertEquals(listOf("kotlin", "mcp"), payload.article.tags)
        assertFalse(payload.article.published)
    }

    @Test
    fun `TEST-MCP-DEVTO-007 maps non OK DevTo response to source compatible exception`() = runTest {
        val client = DevToClient(
            apiKey = "devto-secret",
            httpClient = mockHttpClient {
                respond(
                    content = """{"error":"bad request"}""",
                    status = HttpStatusCode.BadRequest,
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
            },
            articlesUrl = "https://dev.to/api/articles",
        )

        val error = runCatching {
            client.createDraftPost("Bad", "Body", listOf("tag"))
        }.exceptionOrNull()

        assertInstanceOf(DevToPostException::class.java, error)
        assertEquals(
            """Failed to create Dev.to post: 400 Bad Request - {"error":"bad request"}""",
            error?.message,
        )
    }

    private fun mockHttpClient(handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData): HttpClient =
        HttpClient(MockEngine) {
            engine {
                addHandler(handler)
            }
            install(ContentNegotiation) {
                json(json)
            }
        }
}
