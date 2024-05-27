package tech.checkstory

import io.ktor.server.application.*
import io.ktor.server.routing.*

fun Application.routingV1(block: Route.() -> Unit) {
    routing {
        route("v1") {
            this.block()
        }
        // TODO remove when all testers migrate off of v0
        this.block()
    }
}

