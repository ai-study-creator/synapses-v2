package com.synapses.orchestrator.config

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.readLines

const val DEFAULT_LLM_PROVIDER = "openai"

data class AppConfig(
    val slackBotToken: String,
    val slackSigningSecret: String,
    val llmApiKey: String,
    val llmProvider: String = DEFAULT_LLM_PROVIDER,
)

class ConfigException(message: String) : RuntimeException(message)

class AppConfigLoader(
    private val rootDir: Path = Path.of("."),
    private val orchestratorDir: Path = Path.of("orchestrator"),
    private val environment: Map<String, String> = System.getenv(),
) {
    fun load(): AppConfig {
        val values = linkedMapOf<String, String>()
        values.putAll(readDotenv(rootDir.resolve(".env")))
        values.putAll(readDotenv(orchestratorDir.resolve(".env")))
        values.putAll(environment.filterValues { it.isNotBlank() })

        return AppConfig(
            slackBotToken = values.required("SLACK_BOT_TOKEN"),
            slackSigningSecret = values.required("SLACK_SIGNING_SECRET"),
            llmApiKey = values.required("LLM_API_KEY"),
            llmProvider = values["LLM_PROVIDER"]?.takeIf { it.isNotBlank() } ?: DEFAULT_LLM_PROVIDER,
        )
    }

    private fun readDotenv(path: Path): Map<String, String> {
        if (!path.exists() || !Files.isRegularFile(path)) {
            return emptyMap()
        }

        return path.readLines()
            .mapNotNull(::parseDotenvLine)
            .toMap()
    }

    private fun parseDotenvLine(line: String): Pair<String, String>? {
        val trimmed = line.trim()
        if (trimmed.isEmpty() || trimmed.startsWith("#")) {
            return null
        }

        val delimiter = trimmed.indexOf("=")
        if (delimiter <= 0) {
            return null
        }

        val key = trimmed.substring(0, delimiter).trim()
        if (key.isEmpty()) {
            return null
        }

        val rawValue = trimmed.substring(delimiter + 1).trim()
        return key to rawValue.stripMatchingQuotes()
    }

    private fun String.stripMatchingQuotes(): String {
        if (length < 2) {
            return this
        }

        val first = first()
        val last = last()
        return if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
            substring(1, length - 1)
        } else {
            this
        }
    }

    private fun Map<String, String>.required(name: String): String =
        this[name]?.takeIf { it.isNotBlank() }
            ?: throw ConfigException("Missing required configuration value: $name")

}
