package com.synapses.mcpdevto

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

private const val DEVTO_ARTICLES_URL = "https://dev.to/api/articles"

class DevToClient(
    private val apiKey: String,
    private val httpClient: HttpClient = defaultHttpClient(),
    private val articlesUrl: String = DEVTO_ARTICLES_URL,
) {
    suspend fun createDraftPost(title: String, bodyMarkdown: String, tags: List<String>): DevToArticleResponse {
        val response = httpClient.post(articlesUrl) {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            header("api-key", apiKey)
            setBody(
                DevToCreateArticleRequest(
                    article = DevToArticlePayload(
                        title = title,
                        bodyMarkdown = bodyMarkdown,
                        tags = tags,
                        published = false,
                    ),
                ),
            )
        }

        if (!response.status.isSuccess()) {
            val errorBody = response.bodyAsText()
            throw DevToPostException(
                "Failed to create Dev.to post: ${response.status.value} ${response.status.description} - $errorBody",
            )
        }

        return response.body()
    }
}

class DevToPostException(message: String) : RuntimeException(message)

@Serializable
data class DevToCreateArticleRequest(
    val article: DevToArticlePayload,
)

@Serializable
data class DevToArticlePayload(
    val title: String,
    @SerialName("body_markdown")
    val bodyMarkdown: String,
    val tags: List<String>,
    val published: Boolean,
)

@Serializable
data class DevToArticleResponse(
    val id: Long? = null,
    val url: String? = null,
)

fun defaultHttpClient(): HttpClient =
    HttpClient(CIO) {
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    encodeDefaults = true
                    explicitNulls = false
                },
            )
        }
    }

fun successStructuredContent(result: DevToArticleResponse): JsonObject =
    JsonObject(
        buildMap {
            put("success", kotlinx.serialization.json.JsonPrimitive(true))
            result.id?.let { put("postId", kotlinx.serialization.json.JsonPrimitive(it)) }
            result.url?.let { put("url", kotlinx.serialization.json.JsonPrimitive(it)) }
        },
    )

fun errorStructuredContent(message: String): JsonObject =
    JsonObject(
        mapOf(
            "success" to kotlinx.serialization.json.JsonPrimitive(false),
            "error" to kotlinx.serialization.json.JsonPrimitive(message),
        ),
    )
