package com.synapses.orchestrator.app

import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class OrchestratorRuntimeTest {
    @Test
    fun `TEST-ORCH-LIFE-001 root route returns source health payload`() = testApplication {
        application {
            orchestratorModule(
                runtime = OrchestratorRuntime(
                    mcpClientFactory = RecordingMcpClientFactory(),
                    mcpCommand = McpServerCommand("mcp-devto", emptyList(), emptyMap()),
                ),
            )
        }

        val response = client.get("/")

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("""{"message":"Orchestrator Brain is running!"}""", response.bodyAsText())
    }

    @Test
    fun `TEST-ORCH-LIFE-002 startup invokes MCP factory with inherited env and quiet dotenv default`() = runTest {
        val factory = RecordingMcpClientFactory()
        val runtime = OrchestratorRuntime(
            mcpClientFactory = factory,
            mcpCommand = McpServerCommand(
                command = "mcp-devto",
                args = listOf("--stdio"),
                environment = mapOf("DEVTO_API_KEY" to "devto-key"),
            ),
        )

        runtime.start()

        assertEquals("mcp-devto", factory.commands.single().command)
        assertEquals(listOf("--stdio"), factory.commands.single().args)
        assertEquals("devto-key", factory.commands.single().environment["DEVTO_API_KEY"])
        assertEquals("true", factory.commands.single().environment["DOTENV_CONFIG_QUIET"])
        assertNotNull(runtime.mcpClient)
    }

    @Test
    fun `TEST-ORCH-LIFE-003 startup failure keeps runtime available without MCP client`() = runTest {
        val runtime = OrchestratorRuntime(
            mcpClientFactory = RecordingMcpClientFactory(error = IllegalStateException("boom")),
            mcpCommand = McpServerCommand("mcp-devto", emptyList(), emptyMap()),
        )

        runtime.start()

        assertNull(runtime.mcpClient)
    }

    @Test
    fun `TEST-ORCH-LIFE-004 shutdown closes MCP client and clears state`() = runTest {
        val client = RecordingManagedMcpClient()
        val runtime = OrchestratorRuntime(
            mcpClientFactory = RecordingMcpClientFactory(client = client),
            mcpCommand = McpServerCommand("mcp-devto", emptyList(), emptyMap()),
        )

        runtime.start()
        runtime.stop()

        assertEquals(1, client.closeCalls)
        assertNull(runtime.mcpClient)
    }
}
