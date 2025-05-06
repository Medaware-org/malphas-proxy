package org.malphas.proxy

import io.ktor.client.HttpClient
import io.ktor.server.application.*
import io.ktor.server.netty.EngineMain
import org.malphas.proxy.config.loadConfig

fun main(args: Array<String>) {
    EngineMain.main(args)
}

fun Application.module() {
    val config = loadConfig()
    val httpClient = HttpClient()

    configureCors()
    configureRouting(config, httpClient)
}
