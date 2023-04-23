package tech.checkstory.template

import com.eventstore.dbclient.EventData
import com.eventstore.dbclient.EventStoreDBClient
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import tech.checkstory.auth.withFirebasePrincipal
import tech.checkstory.firebase.FIREBASE_AUTH
import tech.checkstory.firebase.user.FirebaseUser
import tech.checkstory.template.task.TemplateTask
import java.util.*

fun Application.configureEventService(client: EventStoreDBClient) {
    routing {
        authenticate(FIREBASE_AUTH) {
            post("/events") {
                withFirebasePrincipal {
                    val domainEventDtos = call.receive<List<DomainEventDto>>()
                    updateStateBasedOnEvents(it, client, domainEventDtos)
                    call.respond(HttpStatusCode.NoContent)
                }
            }
        }
    }
}

suspend fun updateStateBasedOnEvents(
    firebaseUser: FirebaseUser,
    client: EventStoreDBClient,
    events: List<DomainEventDto>
) {
    events
        .sortedBy { it.timestamp }
        .forEach {
            when (it.eventType) {
                else -> {
                    val data = Json.decodeFromJsonElement(TemplateEventData.serializer(), it.data)
                    storeEvent(client, it, data)
                }
            }
        }
}

fun storeEvent(client: EventStoreDBClient, eventDto: DomainEventDto, data: TemplateEventData) {
    val eventData = EventData
        .builderAsJson<Any>(
            UUID.fromString(eventDto.eventId),
            eventDto.eventType,
            data
        ) // event type gets stored within dto - maybe not good!
        .build()
    client.appendToStream("checklist-template-${data.templateId}", eventData)
        .get()
}

@Serializable
data class DomainEventDto(val eventType: String, val eventId: String, val timestamp: Long, val data: JsonObject)

@Serializable
sealed interface TemplateEventData {

    val templateId: String

    fun applyTo(template: Template): Template

    @Serializable
    @SerialName("createTemplate")
    data class CreateTemplateEventData(override val templateId: String) : TemplateEventData {

        override fun applyTo(template: Template): Template {
            return template
        }
    }

    @Serializable
    @SerialName("editTemplateTitle")
    data class EditTemplateTitleEventData(override val templateId: String, val newTitle: String) : TemplateEventData {

        override fun applyTo(template: Template): Template {
            return template.copy(name = newTitle)
        }
    }

    @Serializable
    @SerialName("editTemplateDescription")
    data class EditTemplateDescriptionEventData(override val templateId: String, val newDescription: String) :
        TemplateEventData {

        override fun applyTo(template: Template): Template {
            return template.copy(description = newDescription)
        }
    }

    @Serializable
    @SerialName("addTemplateTask")
    data class AddTemplateTaskEventData(
        override val templateId: String,
        val taskId: String,
        val parentTaskId: String?
    ) : TemplateEventData {

        override fun applyTo(template: Template): Template {
            return template.copy(
                tasks = template.tasks.plus(TemplateTask(taskId, templateId, ""))
            )
        }
    }

    @Serializable
    @SerialName("renameTemplateTask")
    data class RenameTemplateTaskEventData(
        override val templateId: String,
        val taskId: String,
        val newTitle: String,
    ) : TemplateEventData {

        override fun applyTo(template: Template): Template {
            val newTasks = template.tasks.map {
                it.withUpdatedTitleRecursive(taskId, newTitle)
            }
            return template.copy(tasks = newTasks)
        }
    }

    @Serializable
    @SerialName("deleteTemplateTask")
    data class DeleteTemplateTaskData(override val templateId: String, val taskId: String) : TemplateEventData {

        override fun applyTo(template: Template): Template {
            return template.copy(tasks = template.tasks.filterNot { it.id == taskId })
        }
    }
}
