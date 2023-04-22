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
                events.fold(Template(id, "", "")) { acc, event ->
                    event.applyTo(acc)
                }
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
            .fold(Template(id.toString(), "", "")) { acc, checklistTemplateEvent ->
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
    private fun deserializeEvent(event: ResolvedEvent): TemplateEventData? {
        val eventType = event.event.eventType
        val eventData = event.event.eventData
        return when (eventType) {
            "createTemplate" -> {
                Json.decodeFromStream(TemplateEventData.CreateTemplateEventData.serializer(), eventData.inputStream())
            }

            "editTemplateTitle" -> {
                Json.decodeFromStream(
                    TemplateEventData.EditTemplateTitleEventData.serializer(),
                    eventData.inputStream()
                )
            }

            "editTemplateDescription" -> {
                Json.decodeFromStream(
                    TemplateEventData.EditTemplateDescriptionEventData.serializer(),
                    eventData.inputStream()
                )
            }

            "addTemplateTask" -> {
                Json.decodeFromStream(TemplateEventData.AddTemplateTaskEventData.serializer(), eventData.inputStream())
            }

            "renameTemplateTask" -> {
                Json.decodeFromStream(
                    TemplateEventData.EditTemplateTaskTitleEventData.serializer(),
                    eventData.inputStream()
                )
            }

            "deleteTemplateTask" -> {
                Json.decodeFromStream(TemplateEventData.DeleteTemplateTaskData.serializer(), eventData.inputStream())
            }

//            "\$metadata", "\$ProjectionsInitialized" -> null
//            else -> error("Unknown eventType: $eventType")
            else -> null // TODO how to handle just my own events?
        }
    }
}
