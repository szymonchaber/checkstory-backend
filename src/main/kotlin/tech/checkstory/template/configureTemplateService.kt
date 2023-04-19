package tech.checkstory.template

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import tech.checkstory.auth.withFirebasePrincipal
import tech.checkstory.firebase.FIREBASE_AUTH
import java.util.*

fun Application.configureTemplateService(
    templateService: RelationalTemplateRepository,
    eventBasedTemplateRepository: EventBasedTemplateRepository
) {
    routing {
        authenticate(FIREBASE_AUTH) {

            post("/templates") {
                withFirebasePrincipal {
                    val user = call.receive<Template>()
                    val id = templateService.create(user, it)
                    call.respond(HttpStatusCode.Created, id)
                }
            }

            get("/templates") {
                withFirebasePrincipal {
                    val templates = templateService.read(it)
                    call.respond(HttpStatusCode.OK, templates)
                }
            }

            get("/hello") {
                withFirebasePrincipal {
                    call.respond(HttpStatusCode.OK, "Principal has mail: ${it.email} so we allow them to come!")
                }
            }

            get("/templates/{id}") {
                withFirebasePrincipal {
                    val id = call.parameters["id"] ?: throw IllegalArgumentException("Invalid ID")
                    val template = eventBasedTemplateRepository.read(it, UUID.fromString(id))?.let {
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
                withFirebasePrincipal {
                    val id = call.parameters["id"] ?: throw IllegalArgumentException("Invalid ID")
                    templateService.delete(it, UUID.fromString(id))
                    call.respond(HttpStatusCode.OK)
                }
            }

        }
    }
}
