package org.malphas.proxy

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.malphas.proxy.config.ServiceConfiguration

fun Application.configureRouting(config: ServiceConfiguration, httpClient: HttpClient) {
    val redirects = mutableMapOf<String, String>()
    install(Authentication) {
        oauth("auth-oauth-google") {
            urlProvider = { "http://localhost:8080/callback" }
            providerLookup = {
                OAuthServerSettings.OAuth2ServerSettings(
                    name = "google",
                    authorizeUrl = "https://accounts.google.com/o/oauth2/auth",
                    accessTokenUrl = "https://accounts.google.com/o/oauth2/token",
                    requestMethod = HttpMethod.Post,
                    clientId = System.getenv("GOOGLE_CLIENT_ID"),
                    clientSecret = System.getenv("GOOGLE_CLIENT_SECRET"),
                    defaultScopes = listOf("https://www.googleapis.com/auth/userinfo.profile"),
                    onStateCreated = { call, state ->
                        call.request.queryParameters["redirectUrl"]?.let {
                            redirects[state] = it
                        }
                    }
                )
            }
            client = httpClient
        }
    }

    routing {
        get("/") {
            call.respond(HttpStatusCode.OK, "Malphas authentication proxy is up and running.")
        }

        authenticate("auth-oauth-google") {
            get("/login") {}
            get("/callback") {
                val currPrincipal: OAuthAccessTokenResponse.OAuth2? = call.principal()
                val principalState = currPrincipal?.state

                if (currPrincipal == null || principalState == null) {
                    call.application.environment.log.warn("One or both of `currPrincipal` and `principalState` is null. This should not happen.")
                    call.respond(HttpStatusCode.Unauthorized, "Something went wrong.")
                    return@get
                }

                val accessToken = currPrincipal.accessToken

                // Acquire Google User ID
                val req = httpClient.get("https://www.googleapis.com/oauth2/v3/userinfo") {
                    header(HttpHeaders.Authorization, "Bearer $accessToken")
                }
                val userId = Json.parseToJsonElement(req.bodyAsText()).jsonObject["sub"]?.jsonPrimitive?.content!!

                call.application.environment.log.info("Authenticated user with ID $userId")

                call.sessions.set(UserSession(principalState, accessToken, userId))
                redirects[principalState]?.let { redirect ->
                    call.respondRedirect(redirect)
                    return@get
                }
                call.respondRedirect(config.auth.redirect)
            }
        }
        proxyRoute(config, httpClient)
    }
}

enum class RouteType {
    REGULAR,
    PASS,
    CYPRESS
}

fun Route.proxyRoute(config: ServiceConfiguration, client: HttpClient) {
    route("{...}") {
        handle {
            val uri = call.request.uri
            val targetUrl = "${config.backend.host}:${config.backend.port}$uri"
            var type = RouteType.REGULAR

            if (config.proxy.pass.contains(uri))
                type = RouteType.PASS

            val session = call.sessions.get(name = "malphas_session") as UserSession?

            var username = ""
            var userId = ""

            if (session == null && type != RouteType.PASS) {
                call.application.environment.log.info("(DENY) Denied proxy \"${call.request.uri}\" -> \"$targetUrl\" due to lacking permission")
                call.respond(HttpStatusCode.Unauthorized, "This endpoint requires authorization.")
                return@handle
            }

            if (session != null && session.state == "CYPRESS_TESTING") {
                type = RouteType.CYPRESS
                username = "cypress"
                userId = "CYPRESS"
            }

            // Get user info if applicable
            if (type == RouteType.REGULAR) {
                userId = session!!.userId
                val usernameResponse = client.get("https://www.googleapis.com/oauth2/v1/userinfo") {
                    header(HttpHeaders.Authorization, "Bearer ${session!!.token}")
                }
                if (usernameResponse.status != HttpStatusCode.OK) {
                    call.application.environment.log.info("(DENY) The authentication token is invalid.")
                    call.respond(HttpStatusCode.Unauthorized, "This endpoint requires authorization.")
                    return@handle
                }
                username =
                    Json.parseToJsonElement(usernameResponse.bodyAsText()).jsonObject["name"]!!.jsonPrimitive.content
            }

            call.application.environment.log.info("${if (type == RouteType.PASS) "(PASS)" else if (type == RouteType.CYPRESS) "(CYPR)" else "(AUTH)"} Proxy \"${call.request.uri}\" to \"$targetUrl\"")

            // Proxy the request
            val response = client.request(targetUrl) {
                method = call.request.httpMethod

                // Pass the headers along as well
                call.request.headers.entries().forEach { header ->
                    if (header.key.lowercase() == HttpHeaders.TransferEncoding.lowercase() || header.key.lowercase() == HttpHeaders.ContentLength.lowercase())
                        return@forEach

                    header.value.forEach { headers.append(header.key, it) }
                }

                // Apply user info if applicable
                if (type != RouteType.PASS) {
                    headers.append("X-User-ID", userId)
                    headers.append("X-User-Name", username)
                }

                setBody(call.receiveChannel())
            }
            call.respondOutputStream(contentType = response.contentType(), status = response.status) {
                response.bodyAsChannel().copyTo(this)
            }
        }
    }
}
