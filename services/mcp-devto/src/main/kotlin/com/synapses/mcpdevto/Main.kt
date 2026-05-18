package com.synapses.mcpdevto

import io.github.oshai.kotlinlogging.KotlinLoggingConfiguration
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.server.StdioServerTransport
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import io.modelcontextprotocol.kotlin.sdk.types.ServerCapabilities
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.coroutines.runBlocking
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

fun main(): Unit = runBlocking {
    val apiKey = System.getenv("DEVTO_API_KEY")
    if (apiKey.isNullOrBlank()) {
        System.err.print("DEVTO_API_KEY is not set in the environment variables.\n")
        kotlin.system.exitProcess(1)
    }

    try {
        KotlinLoggingConfiguration.logStartupMessage = false
        val server = createMcpDevToServer(DevToClient(apiKey = apiKey))
        val transport = StdioServerTransport(
            System.`in`.asSource().buffered(),
            System.out.asSink().buffered(),
        )
        server.createSession(transport)
        System.err.print("MCP Dev.to Server Started\n")
    } catch (error: Throwable) {
        System.err.print("MCP Dev.to Server failed: ${error.message ?: error.toString()}\n")
        System.exit(1)
    }
}

fun createMcpDevToServer(devToClient: DevToClient): Server =
    Server(
        serverInfo = Implementation(name = "mcp-devto", version = "1.0.0"),
        options = ServerOptions(
            capabilities = ServerCapabilities(
                tools = ServerCapabilities.Tools(listChanged = true),
            ),
        ),
    ) {
        addTool(
            name = "create_dev_post",
            description = "Creates a new draft post on Dev.to.",
            inputSchema = createDevPostToolSchema(),
        ) { request ->
            runCreateDevPostTool(devToClient, request.arguments)
        }
    }

fun createDevPostToolSchema(): ToolSchema =
    ToolSchema(
        properties = buildJsonObject {
            put("title", stringSchema("The title of the post."))
            put("body_markdown", stringSchema("The content of the post in Markdown format."))
            put(
                "tags",
                buildJsonObject {
                    put("type", "array")
                    put("description", "An array of tags for the post.")
                    put(
                        "items",
                        buildJsonObject {
                            put("type", "string")
                        },
                    )
                },
            )
        },
        required = listOf("title", "body_markdown", "tags"),
    )

suspend fun runCreateDevPostTool(devToClient: DevToClient, arguments: JsonObject?): CallToolResult {
    return try {
        val input = parseCreateDevPostInput(arguments)
        val result = devToClient.createDraftPost(
            title = input.title,
            bodyMarkdown = input.bodyMarkdown,
            tags = input.tags,
        )
        val structuredContent = successStructuredContent(result)

        CallToolResult(
            content = listOf(TextContent(text = structuredContent.toString())),
            structuredContent = structuredContent,
        )
    } catch (error: Throwable) {
        val message = error.message ?: error.toString()
        System.err.print("Error creating Dev.to post: $message\n")
        val structuredContent = errorStructuredContent(message)

        CallToolResult(
            content = listOf(TextContent(text = structuredContent.toString())),
            structuredContent = structuredContent,
            isError = true,
        )
    }
}

data class CreateDevPostInput(
    val title: String,
    val bodyMarkdown: String,
    val tags: List<String>,
)

fun parseCreateDevPostInput(arguments: JsonObject?): CreateDevPostInput {
    val input = arguments ?: throw IllegalArgumentException("Tool arguments are required.")

    return CreateDevPostInput(
        title = input.requiredString("title"),
        bodyMarkdown = input.requiredString("body_markdown"),
        tags = input.requiredStringList("tags"),
    )
}

private fun JsonObject.requiredString(name: String): String =
    required(name).jsonPrimitive.contentOrNull
        ?: throw IllegalArgumentException("Tool argument '$name' must be a string.")

private fun JsonObject.requiredStringList(name: String): List<String> {
    val value = required(name)
    return runCatching {
        value.jsonArray.map { item ->
            item.jsonPrimitive.contentOrNull
                ?: throw IllegalArgumentException("Tool argument '$name' must contain only strings.")
        }
    }.getOrElse {
        throw IllegalArgumentException("Tool argument '$name' must be an array of strings.")
    }
}

private fun JsonObject.required(name: String): JsonElement =
    this[name] ?: throw IllegalArgumentException("Tool argument '$name' is required.")

private fun stringSchema(description: String): JsonObject =
    buildJsonObject {
        put("type", "string")
        put("description", description)
    }
