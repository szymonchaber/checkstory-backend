package tech.checkstory.template

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import tech.checkstory.auth.withFirebasePrincipal
import tech.checkstory.firebase.FIREBASE_AUTH
import tech.checkstory.firebase.user.FirebaseUser
import tech.checkstory.template.task.TemplateTask
import tech.checkstory.template.task.TemplateTaskService
import java.util.*

fun Application.configureEventService(templateService: TemplateService, templateTaskService: TemplateTaskService) {
    routing {
        authenticate(FIREBASE_AUTH) {
            post("/events") {
                withFirebasePrincipal {
                    val domainEventDto = call.receive<List<DomainEventDto>>()
                    updateStateBasedOnEvents(it, templateService, templateTaskService, domainEventDto)
                    call.respond(HttpStatusCode.OK)
                }
            }
        }
    }
}

suspend fun updateStateBasedOnEvents(
    firebaseUser: FirebaseUser,
    service: TemplateService,
    templateTaskService: TemplateTaskService,
    events: List<DomainEventDto>
) {
    events
        .sortedBy { it.timestamp }
        .forEach {
            when (it.eventType) {
                "createTemplate" -> {
                    val data = Json.decodeFromJsonElement(CreateTemplateEventData.serializer(), it.data)
                    service.create(UUID.fromString(data.id), Template("", ""), firebaseUser)
                }

                "editTemplateTitle" -> {
                    val data = Json.decodeFromJsonElement(EditTemplateTitleEventData.serializer(), it.data)
                    service.updateTitle(firebaseUser, UUID.fromString(data.id), data.newTitle)
                }

                "addTemplateTask" -> {
                    val data = Json.decodeFromJsonElement(AddTemplateTaskEventData.serializer(), it.data)
                    templateTaskService.create(
                        UUID.fromString(data.taskId),
                        TemplateTask(data.templateId, ""),
                        firebaseUser
                    )
                }

                else -> Unit
            }
        }
}

@Serializable
data class DomainEventDto(val eventType: String, val timestamp: Long, val data: JsonObject)

@Serializable
data class CreateTemplateEventData(val id: String)

@Serializable
data class EditTemplateTitleEventData(val id: String, val newTitle: String)

@Serializable
data class AddTemplateTaskEventData(val templateId: String, val taskId: String, val parentTaskId: String?)
