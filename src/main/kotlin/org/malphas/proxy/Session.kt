package org.malphas.proxy

import io.ktor.server.application.*
import io.ktor.server.sessions.*
import kotlinx.serialization.Serializable

@Serializable
data class UserSession(val state: String, val token: String, val userId: String)

fun Application.configureSessions() {
    install(Sessions) {
        cookie<UserSession>("malphas_session")  {
            cookie.httpOnly = false
        }
    }
}