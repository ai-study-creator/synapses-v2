package com.synapses.orchestrator.app

import io.ktor.http.ContentType
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStarted
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.coroutines.runBlocking

fun Application.orchestratorModule(
    runtime: OrchestratorRuntime = OrchestratorRuntime.default(),
) {
    attributes.put(OrchestratorRuntimeKey, runtime)

    monitor.subscribe(ApplicationStarted) {
        runBlocking { runtime.start() }
    }
    monitor.subscribe(ApplicationStopped) {
        runBlocking { runtime.stop() }
    }

    routing {
        get("/") {
            call.respondText(
                """{"message":"Orchestrator Brain is running!"}""",
                ContentType.Application.Json,
            )
        }
    }

}
