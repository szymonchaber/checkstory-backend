package tech.checkstory.config

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.cors.routing.*

fun Application.cors() {
    install(CORS) {
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Patch)
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.AccessControlAllowOrigin)
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.AcceptEncoding)
        allowHeader(HttpHeaders.UserAgent)
        allowHeader(HttpHeaders.Connection)
        allowHeader(HttpHeaders.Host)
        allowHeader("X-Real-IP")
        allowHeader(HttpHeaders.XForwardedFor)
        allowHeader(HttpHeaders.XForwardedProto)
        allowNonSimpleContentTypes = true
        allowCredentials = true
        allowSameOrigin = true
        anyHost()
    }
}
