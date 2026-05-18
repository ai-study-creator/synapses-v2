package com.synapses.orchestrator.app

import io.ktor.server.application.Application
import io.ktor.util.AttributeKey
import io.modelcontextprotocol.kotlin.sdk.client.Client
import io.modelcontextprotocol.kotlin.sdk.client.StdioClientTransport
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import io.modelcontextprotocol.kotlin.sdk.types.ListToolsResult
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered
import java.nio.file.Path
import kotlin.io.path.Path

val OrchestratorRuntimeKey: AttributeKey<OrchestratorRuntime> = AttributeKey("orchestrator-runtime")

val Application.orchestratorRuntime: OrchestratorRuntime
    get() = attributes[OrchestratorRuntimeKey]

data class McpServerCommand(
    val command: String,
    val args: List<String>,
    val environment: Map<String, String>,
)

interface ManagedMcpClient {
    suspend fun listTools(): ListToolsResult
    suspend fun callTool(name: String, arguments: Map<String, Any?>): CallToolResult
    suspend fun close()
}

interface McpClientFactory {
    suspend fun start(command: McpServerCommand): ManagedMcpClient
}

class OrchestratorRuntime(
    private val mcpClientFactory: McpClientFactory,
    private val mcpCommand: McpServerCommand,
) {
    var mcpClient: ManagedMcpClient? = null
        private set

    suspend fun start() {
        mcpClient = try {
            mcpClientFactory.start(mcpCommand.withQuietDotenv())
        } catch (_: Throwable) {
            null
        }
    }

    suspend fun stop() {
        mcpClient?.close()
        mcpClient = null
    }

    companion object {
        fun default(
            environment: Map<String, String> = System.getenv(),
            projectRoot: Path = Path(System.getProperty("user.dir")),
        ): OrchestratorRuntime =
            OrchestratorRuntime(
                mcpClientFactory = KotlinSdkMcpClientFactory(),
                mcpCommand = defaultMcpDevToCommand(environment, projectRoot),
            )
    }
}

fun McpServerCommand.withQuietDotenv(): McpServerCommand =
    copy(environment = environment + ("DOTENV_CONFIG_QUIET" to (environment["DOTENV_CONFIG_QUIET"] ?: "true")))

fun defaultMcpDevToCommand(
    environment: Map<String, String>,
    projectRoot: Path,
): McpServerCommand {
    val configuredCommand = environment["MCP_DEVTO_COMMAND"]?.trim().orEmpty()
    val configuredArgs = environment["MCP_DEVTO_ARGS"]?.trim().orEmpty()
    if (configuredCommand.isNotEmpty()) {
        return McpServerCommand(
            command = configuredCommand,
            args = configuredArgs.split(" ").filter(String::isNotBlank),
            environment = environment,
        )
    }

    val executableName = if (System.getProperty("os.name").contains("Windows", ignoreCase = true)) {
        "mcp-devto.bat"
    } else {
        "mcp-devto"
    }
    val script = projectRoot.resolve("services/mcp-devto/build/install/mcp-devto/bin/$executableName")
    return McpServerCommand(
        command = script.toString(),
        args = emptyList(),
        environment = environment,
    )
}

class KotlinSdkMcpClientFactory : McpClientFactory {
    override suspend fun start(command: McpServerCommand): ManagedMcpClient {
        val processBuilder = ProcessBuilder(listOf(command.command) + command.args)
        processBuilder.environment().clear()
        processBuilder.environment().putAll(command.environment)

        val process = processBuilder.start()
        val transport = StdioClientTransport(
            input = process.inputStream.asSource().buffered(),
            output = process.outputStream.asSink().buffered(),
            error = process.errorStream.asSource().buffered(),
            classifyStderr = { StdioClientTransport.StderrSeverity.DEBUG },
        )
        val client = Client(Implementation(name = "orchestrator", version = "1.0.0"))

        return try {
            client.connect(transport)
            KotlinSdkManagedMcpClient(client, process)
        } catch (error: Throwable) {
            runCatching { transport.close() }
            process.destroy()
            throw error
        }
    }
}

private class KotlinSdkManagedMcpClient(
    private val client: Client,
    private val process: Process,
) : ManagedMcpClient {
    override suspend fun listTools(): ListToolsResult = client.listTools()

    override suspend fun callTool(name: String, arguments: Map<String, Any?>): CallToolResult =
        client.callTool(name, arguments)

    override suspend fun close() {
        runCatching { client.close() }
        process.destroy()
    }
}
