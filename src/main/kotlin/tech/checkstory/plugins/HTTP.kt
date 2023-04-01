package tech.checkstory.plugins

import io.ktor.server.plugins.defaultheaders.*
import io.ktor.server.plugins.httpsredirect.*
import io.ktor.server.plugins.openapi.*
import io.ktor.server.routing.*
import io.ktor.server.plugins.swagger.*
import io.ktor.server.application.*

fun Application.configureHTTP() {
    install(DefaultHeaders) {
        header("X-Engine", "Ktor") // will send this header with each response
    }
//    install(HttpsRedirect) {
//         The port to redirect to. By default 443, the default HTTPS port.
//        sslPort = 443
//         301 Moved Permanently, or 302 Found redirect.
//        permanentRedirect = true
//    }
    routing {
        openAPI(path = "openapi")
    }
    routing {
        swaggerUI(path = "openapi")
    }
}
