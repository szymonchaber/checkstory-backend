package tech.checkstory.template.api

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import tech.checkstory.auth.firebase.FIREBASE_AUTH
import tech.checkstory.auth.withPrincipal
import tech.checkstory.template.data.RelationalTemplateRepository
import tech.checkstory.template.data.TemplateTaskRepository
import java.util.*

fun Application.configureTemplateTaskService(
    templateService: RelationalTemplateRepository,
    templateTaskRepository: TemplateTaskRepository
) {
    routing {
        authenticate(FIREBASE_AUTH) {
            get("/template/{templateId}/tasks") {
                withPrincipal {
                    val id = call.parameters["templateId"] ?: throw IllegalArgumentException("Invalid ID")
                    val template = templateService.read(UUID.fromString(id))
                    val tasks = templateTaskRepository.getForTemplate(UUID.fromString(id))
                    if (template != null) {
                        call.respond(HttpStatusCode.OK, tasks)
                    } else {
                        call.respond(HttpStatusCode.NotFound)
                    }
                }
            }
        }
    }
}