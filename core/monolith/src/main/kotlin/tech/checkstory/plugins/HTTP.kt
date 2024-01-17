package tech.checkstory.plugins

import io.ktor.server.application.*
import io.ktor.server.plugins.defaultheaders.*

fun Application.configureHTTP() {
    install(DefaultHeaders) {
        header("X-Engine", "Ktor")
    }
//    install(HttpsRedirect) {
//         The port to redirect to. By default 443, the default HTTPS port.
//        sslPort = 443
//         301 Moved Permanently, or 302 Found redirect.
//        permanentRedirect = true
//    }
//    routing {
//        openAPI(path = "openapi")
//    }
//    routing {
//        swaggerUI(path = "openapi")
//    }
}
