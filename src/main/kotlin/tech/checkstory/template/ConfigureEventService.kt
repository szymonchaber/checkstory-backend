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
import kotlinx.serialization.Transient
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
                    val domainEventDtos = call.receive<List<DomainEvent>>()
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
    events: List<DomainEvent>
) {
    events
        .sortedBy { it.timestamp }
        .forEach {
            when (it) {
                is TemplateEvent -> storeTemplateEvent(client, it)
            }
        }
}

fun storeTemplateEvent(client: EventStoreDBClient, templateEvent: TemplateEvent) {
    val eventData = EventData
        .builderAsJson<Any>(
            UUID.fromString(templateEvent.eventId),
            templateEvent.eventType,
            templateEvent
        ) // event type gets stored within dto - maybe not good!
        .build()
    client.appendToStream("checklist-template-${templateEvent.templateId}", eventData)
        .get()
}

@Serializable
sealed interface DomainEvent {

    @Transient
    val eventType: String
    val eventId: String
    val timestamp: Long
}

@Serializable
sealed interface TemplateEvent : DomainEvent {

    val templateId: String

    fun applyTo(template: Template): Template

    @Serializable
    @SerialName("createTemplate")
    data class CreateTemplateEvent(
        override val templateId: String,
        override val eventId: String,
        override val timestamp: Long
    ) : TemplateEvent {

        override val eventType: String = "createTemplate"

        override fun applyTo(template: Template): Template {
            return template
        }
    }

    @Serializable
    @SerialName("editTemplateTitle")
    data class EditTemplateTitleEvent(
        override val templateId: String,
        val newTitle: String,
        override val eventId: String,
        override val timestamp: Long
    ) : TemplateEvent {

        override val eventType: String = "editTemplateTitle"

        override fun applyTo(template: Template): Template {
            return template.copy(name = newTitle)
        }
    }

    @Serializable
    @SerialName("editTemplateDescription")
    data class EditTemplateDescriptionEvent(
        override val templateId: String,
        val newDescription: String,
        override val eventId: String,
        override val timestamp: Long
    ) : TemplateEvent {

        override val eventType: String = "editTemplateDescription"

        override fun applyTo(template: Template): Template {
            return template.copy(description = newDescription)
        }
    }

    @Serializable
    @SerialName("addTemplateTask")
    data class AddTemplateTaskEvent(
        override val templateId: String,
        val taskId: String,
        val parentTaskId: String?,
        override val eventId: String,
        override val timestamp: Long
    ) : TemplateEvent {

        override val eventType: String = "addTemplateTask"

        override fun applyTo(template: Template): Template {
            return template.copy(
                tasks = template.tasks.plus(TemplateTask(taskId, templateId, ""))
            )
        }
    }

    @Serializable
    @SerialName("renameTemplateTask")
    data class RenameTemplateTaskEvent(
        override val templateId: String,
        val taskId: String,
        val newTitle: String,
        override val eventId: String,
        override val timestamp: Long
    ) : TemplateEvent {

        override val eventType: String = "renameTemplateTask"

        override fun applyTo(template: Template): Template {
            val newTasks = template.tasks.map {
                it.withUpdatedTitleRecursive(taskId, newTitle)
            }
            return template.copy(tasks = newTasks)
        }
    }

    @Serializable
    @SerialName("deleteTemplateTask")
    data class DeleteTemplateTask(
        override val templateId: String,
        val taskId: String,
        override val eventId: String,
        override val timestamp: Long
    ) : TemplateEvent {

        override val eventType: String = "deleteTemplateTask"

        override fun applyTo(template: Template): Template {
            return template.copy(tasks = template.tasks.filterNot { it.id == taskId })
        }
    }
}
