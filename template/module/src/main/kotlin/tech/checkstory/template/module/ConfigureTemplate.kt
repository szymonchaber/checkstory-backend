package tech.checkstory.template.module

import io.ktor.server.application.*
import org.kodein.di.DI
import org.kodein.di.instance
import tech.checkstory.template.data.EventSourcedTemplateRepository
import tech.checkstory.template.data.RelationalTemplateRepository
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import tech.checkstory.auth.firebase.FIREBASE_AUTH
import tech.checkstory.auth.withPrincipal
import java.util.*

context(DI)
fun Application.configureTemplate() {
    val templateService by instance<RelationalTemplateRepository>()
    val eventSourcedTemplateRepository by instance<EventSourcedTemplateRepository>()
    configureTemplateService(templateService, eventSourcedTemplateRepository)
}

fun Application.configureTemplateService(
    relationalTemplateRepository: RelationalTemplateRepository,
    eventSourcedTemplateRepository: EventSourcedTemplateRepository,
) {
    routing {
        authenticate(FIREBASE_AUTH) {
            get("/templates") {
                withPrincipal {
                    val templates = relationalTemplateRepository.readAll()
                    call.respond(HttpStatusCode.OK, templates)
                }
            }

            get("/templates/{id}") {
                withPrincipal {
                    val id = call.parameters["id"] ?: throw IllegalArgumentException("Invalid ID")
                    val template = eventSourcedTemplateRepository.read(UUID.fromString(id))
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
