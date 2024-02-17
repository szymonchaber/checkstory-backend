package tech.checkstory.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.sentry.Sentry

fun Application.configureRouting() {
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            Sentry.captureException(cause)
            call.respondText(text = "500: ${cause.stackTraceToString()}", status = HttpStatusCode.InternalServerError)
        }
    }
}
