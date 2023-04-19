package tech.checkstory.template

import com.eventstore.dbclient.EventData
import com.eventstore.dbclient.EventStoreDBClient
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

fun Application.configureEventService(
    templateService: RelationalTemplateRepository,
    templateTaskService: TemplateTaskService,
    client: EventStoreDBClient
) {
    routing {
        authenticate(FIREBASE_AUTH) {
            post("/events") {
                withFirebasePrincipal {
                    val domainEventDtos = call.receive<List<DomainEventDto>>()
                    updateStateBasedOnEvents(it, client, templateService, templateTaskService, domainEventDtos)
                    call.respond(HttpStatusCode.OK)
                }
            }
        }
    }
}

suspend fun updateStateBasedOnEvents(
    firebaseUser: FirebaseUser,
    client: EventStoreDBClient,
    service: RelationalTemplateRepository,
    templateTaskService: TemplateTaskService,
    events: List<DomainEventDto>
) {
    events
        .sortedBy { it.timestamp }
        .forEach {
            when (it.eventType) {
                "createTemplate" -> {
                    val data =
                        Json.decodeFromJsonElement(TemplateEventData.CreateTemplateEventData.serializer(), it.data)
                    storeEvent(client, it, data)
                    service.create(Template(data.templateId, "", ""), firebaseUser)
                }

                "editTemplateTitle" -> {
                    val data =
                        Json.decodeFromJsonElement(TemplateEventData.EditTemplateTitleEventData.serializer(), it.data)
                    storeEvent(client, it, data)
                    service.updateTitle(firebaseUser, UUID.fromString(data.templateId), data.newTitle)
                }

                "addTemplateTask" -> {
                    val data =
                        Json.decodeFromJsonElement(TemplateEventData.AddTemplateTaskEventData.serializer(), it.data)
                    storeEvent(client, it, data)
                    templateTaskService.create(
                        UUID.fromString(data.taskId),
                        TemplateTask(data.templateId, ""),
                        firebaseUser
                    )
                }

                "deleteTemplateTask" -> {
                    val data =
                        Json.decodeFromJsonElement(TemplateEventData.DeleteTemplateTaskData.serializer(), it.data)
                    storeEvent(client, it, data)
                    templateTaskService.delete(firebaseUser, UUID.fromString(data.taskId))
                }

                else -> Unit
            }
        }
}

fun storeEvent(client: EventStoreDBClient, eventDto: DomainEventDto, data: TemplateEventData) {
    val eventData = EventData
        .builderAsJson<Any>(eventDto.eventType, data) // event type gets stored within dto - maybe not good!
        .build()
    client.appendToStream("checklist-template-${data.templateId}", eventData)
        .get()
}

@Serializable
data class DomainEventDto(val eventType: String, val eventId: String, val timestamp: Long, val data: JsonObject)

sealed interface TemplateEventData {

    val templateId: String

    fun applyTo(template: Template): Template

    @Serializable
    data class CreateTemplateEventData(override val templateId: String) : TemplateEventData {

        override fun applyTo(template: Template): Template {
            return template
        }
    }

    @Serializable
    data class EditTemplateTitleEventData(override val templateId: String, val newTitle: String) : TemplateEventData {

        override fun applyTo(template: Template): Template {
            return template.copy(name = newTitle)
        }
    }

    @Serializable
    data class AddTemplateTaskEventData(
        override val templateId: String,
        val taskId: String,
        val parentTaskId: String?
    ) : TemplateEventData {

        override fun applyTo(template: Template): Template {
            return template
        }
    }

    @Serializable
    data class DeleteTemplateTaskData(override val templateId: String, val taskId: String) : TemplateEventData {

        override fun applyTo(template: Template): Template {
            return template
        }
    }
}