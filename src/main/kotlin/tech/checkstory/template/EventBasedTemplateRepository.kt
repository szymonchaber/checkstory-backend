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
//        .maxCount(10)
    }

    @OptIn(ExperimentalSerializationApi::class)
    private fun deserializeEvent(event: ResolvedEvent): TemplateEvent? {
        val eventType = event.event.eventType
        val eventData = event.event.eventData
        return when (eventType) {
            "createTemplate" -> {
                Json.decodeFromStream(TemplateEvent.CreateTemplateEvent.serializer(), eventData.inputStream())
            }

            "editTemplateTitle" -> {
                Json.decodeFromStream(
                    TemplateEvent.EditTemplateTitleEvent.serializer(),
                    eventData.inputStream()
                )
            }

            "editTemplateDescription" -> {
                Json.decodeFromStream(
                    TemplateEvent.EditTemplateDescriptionEvent.serializer(),
                    eventData.inputStream()
                )
            }

            "addTemplateTask" -> {
                Json.decodeFromStream(TemplateEvent.AddTemplateTaskEvent.serializer(), eventData.inputStream())
            }

            "renameTemplateTask" -> {
                Json.decodeFromStream(
                    TemplateEvent.RenameTemplateTaskEvent.serializer(),
                    eventData.inputStream()
                )
            }

            "deleteTemplateTask" -> {
                Json.decodeFromStream(TemplateEvent.DeleteTemplateTaskEvent.serializer(), eventData.inputStream())
            }

//            "\$metadata", "\$ProjectionsInitialized" -> null
//            else -> error("Unknown eventType: $eventType")
            else -> null // TODO how to handle just my own events?
        }
    }
}

class TemplateAggregate(private val events: List<TemplateEvent>) {

    init {
        require(events.isEmpty() || events.first() is TemplateEvent.CreateTemplateEvent)
    }
}
