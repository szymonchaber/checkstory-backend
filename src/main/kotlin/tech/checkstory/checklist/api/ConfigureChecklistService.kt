package tech.checkstory.checklist.api

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import tech.checkstory.auth.firebase.FIREBASE_AUTH
import tech.checkstory.auth.withFirebasePrincipal
import tech.checkstory.checklist.data.EventBasedChecklistRepository
import java.util.*

fun Application.configureChecklistService(
    eventBasedChecklistRepository: EventBasedChecklistRepository
) {
    routing {
        authenticate(FIREBASE_AUTH) {
            get("/checklists") {
                withFirebasePrincipal {
                    val checklists = eventBasedChecklistRepository.readAll(it)
                    call.respond(HttpStatusCode.OK, checklists)
                }
            }

            get("/checklists/{id}") {
                withFirebasePrincipal {
                    val id = call.parameters["id"] ?: throw IllegalArgumentException("Invalid ID")
                    val template = eventBasedChecklistRepository.read(it, UUID.fromString(id))
                    if (template != null) {
                        call.respond(HttpStatusCode.OK, template)
                    } else {
                        call.respond(HttpStatusCode.NotFound)
                    }
                }
            }
        }
    }
}
