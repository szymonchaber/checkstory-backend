package tech.checkstory.template.data

import com.eventstore.dbclient.EventStoreDBClient
import com.eventstore.dbclient.ReadAllOptions
import com.eventstore.dbclient.ReadStreamOptions
import com.eventstore.dbclient.ResolvedEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import tech.checkstory.firebase.user.FirebaseUser
import tech.checkstory.template.domain.TemplateRepository
import tech.checkstory.template.domain.aggregate.Template
import tech.checkstory.template.domain.event.TemplateEvent
import tech.checkstory.template.domain.event.TemplateEvent.CreateTemplateEvent
import tech.checkstory.template.extensions.requireElementOfType
import java.util.*

class EventBasedTemplateRepository(private val client: EventStoreDBClient) : TemplateRepository {

    override suspend fun readAll(firebaseUser: FirebaseUser): List<Template> {
        val result = withContext(Dispatchers.IO) {
            client.readAll(ReadAllOptions.get().fromStart().forwards()).get()
        }
        return result.events
            .asSequence()
            .mapNotNull(::deserializeEvent)
            .groupBy {
                it.templateId
            }.map { (_, events) ->
                val (createTemplate, otherEvents) = events.requireElementOfType<TemplateEvent, CreateTemplateEvent>()
                TemplateAggregate(createTemplate, otherEvents).template
            }
            .filter {
                it.userId == firebaseUser.userId
            }
            .filterNot {
                it.isDeleted
            }
            .toList()
    }

    override suspend fun read(firebaseUser: FirebaseUser, id: UUID): Template? {
        val result = withContext(Dispatchers.IO) {
            client.readStream("checklist-template-$id", readStreamOptions()).get()
        }
        if (result.events.isEmpty()) {
            return null
        }
        return result.events
            .mapNotNull(::deserializeEvent)
            .fold(
                Template(
                    id = id,
                    userId = "",
                    name = "",
                    description = ""
                )
            ) { acc, checklistTemplateEvent ->
                checklistTemplateEvent.applyTo(acc)
            }
    }

    private fun readStreamOptions(): ReadStreamOptions {
        return ReadStreamOptions.get()
            .forwards()
            .fromStart()
    }

    @OptIn(ExperimentalSerializationApi::class)
    private fun deserializeEvent(event: ResolvedEvent): TemplateEvent? {
        val eventType = event.event.eventType
        val eventData = event.event.eventData
        return when (eventType) {
            in knownTemplateEventTypes -> {
                Json.decodeFromStream(TemplateEvent.serializer(), eventData.inputStream())
            }

            else -> null
        }
    }
}

class TemplateAggregate(
    createTemplateEvent: CreateTemplateEvent,
    otherEvents: List<TemplateEvent>
) {

    val template: Template

    init {
        val createdTemplate = Template(
            id = createTemplateEvent.templateId,
            userId = createTemplateEvent.userId,
            "",
            "",
        )
        template = otherEvents.fold(createdTemplate) { acc, event ->
            event.applyTo(acc)
        }
    }
}

val knownTemplateEventTypes = setOf(
    "createTemplate",
    "editTemplateTitle",
    "editTemplateDescription",
    "addTemplateTask",
    "renameTemplateTask",
    "deleteTemplateTask",
    "updateTemplateTasksPositions",
    "moveTemplateTask",
    "addOrUpdateTemplateReminder",
    "deleteTemplateReminder",
    "deleteTemplate"
)