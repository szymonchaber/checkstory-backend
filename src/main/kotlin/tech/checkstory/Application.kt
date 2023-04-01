package tech.checkstory

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import tech.checkstory.city.configureCityService
import tech.checkstory.plugins.*
import tech.checkstory.user.configureUserService

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    configureSecurity()
    configureHTTP()
    configureMonitoring()
    configureSerialization()
    configureUserService()
    configureCityService()
    configureRouting()
}
