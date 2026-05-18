package com.synapses.mcpdevto

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.put
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CreateDevPostToolTest {
    @Test
    fun `TEST-MCP-DEVTO-003 exposes source compatible tool schema`() {
        val schema = createDevPostToolSchema()

        assertEquals(listOf("title", "body_markdown", "tags"), schema.required)
        assertNotNull(schema.properties?.get("title"))
        assertNotNull(schema.properties?.get("body_markdown"))
        assertNotNull(schema.properties?.get("tags"))
    }

    @Test
    fun `TEST-MCP-DEVTO-004 rejects invalid input before DevTo API call`() = runTest {
        var calledDevTo = false
        val client = DevToClient(
            apiKey = "devto-secret",
            httpClient = HttpClient(MockEngine) {
                engine {
                    addHandler {
                        calledDevTo = true
                        respond("""{"id":123,"url":"https://dev.to/example/post"}""")
                    }
                }
                install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            },
        )

        val result = runCreateDevPostTool(
            client,
            buildJsonObject {
                put("title", "Title")
                put("body_markdown", "Body")
                put("tags", "not-an-array")
            },
        )

        assertTrue(result.isError == true)
        assertEquals(false, result.structuredContent?.get("success")?.jsonPrimitive?.boolean)
        assertEquals(false, calledDevTo)
    }

    @Test
    fun `TEST-MCP-DEVTO-006 returns source compatible MCP success content`() = runTest {
        val client = DevToClient(
            apiKey = "devto-secret",
            httpClient = HttpClient(MockEngine) {
                engine {
                    addHandler {
                        respond(
                            content = """{"id":123,"url":"https://dev.to/example/post"}""",
                            status = HttpStatusCode.Created,
                            headers = headersOf(HttpHeaders.ContentType, "application/json"),
                        )
                    }
                }
                install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            },
        )

        val result = runCreateDevPostTool(
            client,
            buildJsonObject {
                put("title", "Title")
                put("body_markdown", "Body")
                put(
                    "tags",
                    buildJsonArray {
                        add(kotlinx.serialization.json.JsonPrimitive("kotlin"))
                    },
                )
            },
        )

        assertEquals(true, result.structuredContent?.get("success")?.jsonPrimitive?.boolean)
        assertEquals(123L, result.structuredContent?.get("postId")?.jsonPrimitive?.long)
        assertEquals("https://dev.to/example/post", result.structuredContent?.get("url")?.jsonPrimitive?.contentOrNull)
        assertEquals(result.structuredContent.toString(), (result.content.first() as TextContent).text)
    }
}
