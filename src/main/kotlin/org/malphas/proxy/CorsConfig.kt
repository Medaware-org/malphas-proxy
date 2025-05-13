package org.malphas.proxy

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*

class MalphasCorsConfiguration {
    var origin: String = "*"
}

val MalphasCors = createApplicationPlugin("MalphasCORS", createConfiguration = ::MalphasCorsConfiguration) {
    onCall { call ->
        call.response.headers.append(HttpHeaders.AccessControlAllowOrigin, pluginConfig.origin)
        call.response.headers.append(HttpHeaders.AccessControlAllowHeaders, "Accept, Accept-Language, Content-Language, Content-Type, Authorization, Origin, X-Requested-With, Access-Control-Request-Method, Access-Control-Request-Headers, Pragma, Cache-Control, Expires, If-Modified-Since, If-None-Match, Vary, Link, X-Forwarded-For, X-Forwarded-Proto")
        call.response.headers.append(HttpHeaders.AccessControlAllowMethods, "GET, POST, PUT, DELETE, OPTIONS")
        call.response.headers.append(HttpHeaders.AccessControlAllowCredentials, "true")
        if (call.request.httpMethod == HttpMethod.Options)
            call.respond(HttpStatusCode.OK, "OK")
    }
}