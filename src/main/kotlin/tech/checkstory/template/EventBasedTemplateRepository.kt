package tech.checkstory.template

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
import java.util.*

class EventBasedTemplateRepository(private val client: EventStoreDBClient) : TemplateRepository {

    override suspend fun readAll(firebaseUser: FirebaseUser): List<Template> {
        val result = withContext(Dispatchers.IO) {
            client.readAll(ReadAllOptions.get().fromStart().forwards()).get()
        }
        if (result.events.isEmpty()) {
            return listOf()
        }
        return result.events
            .mapNotNull(::deserializeEvent)
            .groupBy {
                it.templateId
            }.map { (id, events) ->
                require(events.first() is TemplateEvent.CreateTemplateEvent) {
                    "First event must be a CreateTemplateEvent. Was: ${events.first()}"
                }
                events.fold(Template(id, "", "", "")) { acc, event ->
                    event.applyTo(acc)
                }
            }
            .filter {
                it.userId == firebaseUser.userId
            }
            .filterNot {
                it.isDeleted
            }
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
                    id = id.toString(),
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

class TemplateAggregate(private val events: List<TemplateEvent>) {

    init {
        require(events.isEmpty() || events.first() is TemplateEvent.CreateTemplateEvent)
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