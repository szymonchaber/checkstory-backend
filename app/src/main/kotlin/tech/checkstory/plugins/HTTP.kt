package tech.checkstory.plugins

import io.ktor.server.application.*
import io.ktor.server.plugins.defaultheaders.*
import io.ktor.server.routing.*

fun Application.configureHTTP() {
    install(DefaultHeaders) {
        header("X-Engine", "Ktor")
    }
    install(IgnoreTrailingSlash)
//    routing {
//        openAPI(path = "openapi")
//    }
//    routing {
//        swaggerUI(path = "openapi")
//    }
}
