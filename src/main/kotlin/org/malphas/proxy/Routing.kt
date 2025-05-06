package org.malphas.proxy

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.*
import io.ktor.utils.io.jvm.javaio.*
import org.apache.http.client.methods.HttpHead
import org.malphas.proxy.config.MalphasProxyConfig

fun Application.configureRouting(config: MalphasProxyConfig, client: HttpClient) {
    routing {
        get("/") {
            call.respond(HttpStatusCode.OK, "Malphas authentication proxy is up and running.")
        }

        proxyRoute(config, client)
    }
}

fun Route.proxyRoute(config: MalphasProxyConfig, client: HttpClient) {
    route("{...}") {
        handle {
            val targetUrl = "${config.malphas.host}:${config.malphas.port}${call.request.uri}"
            call.application.environment.log.info("Proxy \"${call.request.uri}\" to $targetUrl")
            val response = client.request(targetUrl) {
                method = call.request.httpMethod
                call.request.headers.entries().forEach { header ->
                    if (header.key == HttpHeaders.TransferEncoding || header.key == HttpHeaders.ContentLength)
                        return@forEach

                    header.value.forEach { headers.append(header.key, it) }
                }
                setBody(call.receiveChannel())
            }
            call.respondOutputStream(contentType = response.contentType(), status = response.status) {
                response.bodyAsChannel().copyTo(this)
            }
        }
    }
}
