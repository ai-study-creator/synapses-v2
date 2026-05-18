package com.synapses.orchestrator.app

import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.ListToolsResult
import kotlinx.serialization.json.JsonObject

class RecordingManagedMcpClient : ManagedMcpClient {
    var closeCalls = 0

    override suspend fun listTools(): ListToolsResult =
        ListToolsResult(tools = emptyList())

    override suspend fun callTool(name: String, arguments: Map<String, Any?>): CallToolResult =
        CallToolResult(
            content = emptyList(),
            isError = false,
            structuredContent = JsonObject(emptyMap()),
            meta = JsonObject(emptyMap()),
        )

    override suspend fun close() {
        closeCalls += 1
    }
}

class RecordingMcpClientFactory(
    private val client: ManagedMcpClient = RecordingManagedMcpClient(),
    private val error: Throwable? = null,
) : McpClientFactory {
    val commands = mutableListOf<McpServerCommand>()

    override suspend fun start(command: McpServerCommand): ManagedMcpClient {
        commands += command
        error?.let { throw it }
        return client
    }
}
