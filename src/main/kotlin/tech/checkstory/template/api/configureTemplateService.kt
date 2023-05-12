package tech.checkstory.template.api

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import tech.checkstory.auth.firebase.FIREBASE_AUTH
import tech.checkstory.auth.withPrincipal
import tech.checkstory.template.data.EventSourcedTemplateRepository
import tech.checkstory.template.data.RelationalTemplateRepository
import tech.checkstory.template.domain.aggregate.Template
import java.util.*

fun Application.configureTemplateService(
    templateService: RelationalTemplateRepository,
    eventSourcedTemplateRepository: EventSourcedTemplateRepository
) {
    routing {
        authenticate(FIREBASE_AUTH) {

            post<Template>("/templates") {
                withPrincipal {
                    val id = templateService.create(it)
                    call.respond(HttpStatusCode.Created, id)
                }
            }

            get("/templates") {
                withPrincipal {
                    val templates = eventSourcedTemplateRepository.readAll()
                    call.respond(HttpStatusCode.OK, templates)
                }
            }

            get("/templates/{id}") {
                withPrincipal {
                    val id = call.parameters["id"] ?: throw IllegalArgumentException("Invalid ID")
                    val template = eventSourcedTemplateRepository.read(UUID.fromString(id))?.let {
                        it.copy(name = "Event sourced " + it.name)
                    }
                    if (template != null) {
                        call.respond(HttpStatusCode.OK, template)
                    } else {
                        call.respond(HttpStatusCode.NotFound)
                    }
                }
            }


//            put("/templates/{id}") {
//                withFirebasePrincipal {
//                    val user = call.receive<Template>()
//                    templateService.update(it.userId, user)
//                    call.respond(HttpStatusCode.OK)
//                }
//            }

            delete("/templates/{id}") {
                withPrincipal {
                    val id = call.parameters["id"] ?: throw IllegalArgumentException("Invalid ID")
                    templateService.delete(UUID.fromString(id))
                    call.respond(HttpStatusCode.OK)
                }
            }

        }
    }
}
