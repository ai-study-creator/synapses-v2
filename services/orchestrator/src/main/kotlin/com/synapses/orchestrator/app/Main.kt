package com.synapses.orchestrator.app

import com.synapses.orchestrator.config.AppConfig
import com.synapses.orchestrator.config.AppConfigLoader
import com.synapses.orchestrator.llm.LlmClient
import com.synapses.orchestrator.slack.CoroutineSlackAppMentionScheduler
import com.synapses.orchestrator.slack.SlackAppMentionProcessor
import com.synapses.orchestrator.slack.SlackJavaSdkMessageClient
import com.synapses.orchestrator.slack.slackEventsRoutes
import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import kotlin.io.path.Path

fun main() {
    val environment = System.getenv()
    val config = AppConfigLoader(
        rootDir = Path("."),
        orchestratorDir = Path("orchestrator"),
        environment = environment,
    ).load()
    val port = environment["PORT"]?.toIntOrNull() ?: 8000

    embeddedServer(
        factory = Netty,
        host = "0.0.0.0",
        port = port,
        module = { productionOrchestratorModule(config, environment) },
    ).start(wait = true)
}

fun Application.productionOrchestratorModule(
    config: AppConfig,
    environment: Map<String, String> = System.getenv(),
) {
    val runtime = OrchestratorRuntime.default(environment = environment)
    val llmClient = LlmClient(
        provider = config.llmProvider,
        apiKey = config.llmApiKey,
        environment = environment,
    )
    val processor = SlackAppMentionProcessor(
        llmClient = llmClient,
        slackClient = SlackJavaSdkMessageClient(config.slackBotToken),
    )

    orchestratorModule(runtime = runtime)
    slackEventsRoutes(
        signingSecret = config.slackSigningSecret,
        processor = CoroutineSlackAppMentionScheduler(processor),
    )
}
