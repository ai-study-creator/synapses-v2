package com.synapses.orchestrator.config

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText

class AppConfigLoaderTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `TEST-CONFIG-001 loads root and orchestrator dotenv files and ignores extras`() {
        tempDir.resolve(".env").writeText(
            """
            SLACK_BOT_TOKEN=root-token
            SLACK_SIGNING_SECRET=root-secret
            LLM_API_KEY=root-llm
            IGNORED_VALUE=ignored
            """.trimIndent(),
        )
        val orchestratorDir = tempDir.resolve("orchestrator").createDirectories()
        orchestratorDir.resolve(".env").writeText(
            """
            SLACK_SIGNING_SECRET=orchestrator-secret
            LLM_PROVIDER=github_models
            """.trimIndent(),
        )

        val config = AppConfigLoader(
            rootDir = tempDir,
            orchestratorDir = orchestratorDir,
            environment = emptyMap(),
        ).load()

        assertEquals("root-token", config.slackBotToken)
        assertEquals("orchestrator-secret", config.slackSigningSecret)
        assertEquals("root-llm", config.llmApiKey)
        assertEquals("github_models", config.llmProvider)
    }

    @Test
    fun `TEST-CONFIG-002 environment values override dotenv values`() {
        tempDir.resolve(".env").writeText(
            """
            SLACK_BOT_TOKEN=root-token
            SLACK_SIGNING_SECRET=root-secret
            LLM_API_KEY=root-llm
            LLM_PROVIDER=github_models
            """.trimIndent(),
        )
        val orchestratorDir = tempDir.resolve("orchestrator").createDirectories()

        val config = AppConfigLoader(
            rootDir = tempDir,
            orchestratorDir = orchestratorDir,
            environment = mapOf(
                "SLACK_BOT_TOKEN" to "env-token",
                "LLM_PROVIDER" to "openai",
            ),
        ).load()

        assertEquals("env-token", config.slackBotToken)
        assertEquals("root-secret", config.slackSigningSecret)
        assertEquals("root-llm", config.llmApiKey)
        assertEquals("openai", config.llmProvider)
    }

    @Test
    fun `TEST-CONFIG-003 requires Slack and LLM values`() {
        val orchestratorDir = tempDir.resolve("orchestrator")
        Files.createDirectories(orchestratorDir)

        val error = assertThrows(ConfigException::class.java) {
            AppConfigLoader(
                rootDir = tempDir,
                orchestratorDir = orchestratorDir,
                environment = emptyMap(),
            ).load()
        }

        assertEquals("Missing required configuration value: SLACK_BOT_TOKEN", error.message)
    }

    @Test
    fun `TEST-CONFIG-004 defaults LLM provider to openai`() {
        val orchestratorDir = tempDir.resolve("orchestrator").createDirectories()

        val config = AppConfigLoader(
            rootDir = tempDir,
            orchestratorDir = orchestratorDir,
            environment = mapOf(
                "SLACK_BOT_TOKEN" to "env-token",
                "SLACK_SIGNING_SECRET" to "env-secret",
                "LLM_API_KEY" to "env-llm",
            ),
        ).load()

        assertEquals("openai", config.llmProvider)
    }
}
