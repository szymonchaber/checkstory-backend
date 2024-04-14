package tech.checkstory.plugins

import io.ktor.server.application.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.request.*

fun Application.configureMonitoring() {
    install(CallLogging) {
        filter { call ->
            call.request.path().startsWith("/")
        }
    }
}
