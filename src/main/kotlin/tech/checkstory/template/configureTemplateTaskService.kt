package tech.checkstory.template

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import tech.checkstory.auth.withFirebasePrincipal
import tech.checkstory.firebase.FIREBASE_AUTH
import tech.checkstory.template.task.TemplateTaskService
import java.util.*

fun Application.configureTemplateTaskService(
    templateService: RelationalTemplateRepository,
    templateTaskService: TemplateTaskService
) {
    routing {
        authenticate(FIREBASE_AUTH) {
            get("/template/{templateId}/tasks") {
                withFirebasePrincipal {
                    val id = call.parameters["templateId"] ?: throw IllegalArgumentException("Invalid ID")
                    val template = templateService.read(it, UUID.fromString(id))
                    val tasks = templateTaskService.getForTemplate(it, UUID.fromString(id))
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
