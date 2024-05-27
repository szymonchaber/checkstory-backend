package tech.checkstory.checklist

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.kodein.di.DI
import org.kodein.di.instance
import tech.checkstory.auth.firebase.authenticateFirebase
import tech.checkstory.auth.withPrincipal
import tech.checkstory.checklist.data.ChecklistRepository
import tech.checkstory.routingV1
import java.util.*

context(DI)
fun Application.configureChecklistService() {
    val checklistRepository by instance<ChecklistRepository>()
    routingV1 {
        authenticateFirebase {
            get("/checklists") {
                withPrincipal {
                    val checklists = checklistRepository.readAllNotDeleted()
                    call.respond(HttpStatusCode.OK, checklists)
                }
            }

            get("/checklists/{id}") {
                withPrincipal {
                    val id = call.parameters["id"] ?: throw IllegalArgumentException("Invalid ID")
                    val template = checklistRepository.read(UUID.fromString(id))
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
