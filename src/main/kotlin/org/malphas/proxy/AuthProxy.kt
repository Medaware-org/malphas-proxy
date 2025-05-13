package org.malphas.proxy

import io.ktor.client.*
import io.ktor.server.application.*
import io.ktor.server.netty.*
import org.malphas.proxy.config.loadConfig

fun main(args: Array<String>) {
    EngineMain.main(args)
}

fun Application.module() {
    val config = loadConfig()
    val httpClient = HttpClient()

    configureSessions()

    install(MalphasCors) {
        origin = config.frontend.origin
    }

    configureRouting(config, httpClient)
}
