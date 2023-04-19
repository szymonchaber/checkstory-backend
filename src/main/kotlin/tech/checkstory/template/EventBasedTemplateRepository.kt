package tech.checkstory.template

import com.eventstore.dbclient.EventStoreDBClient
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

    override suspend fun read(firebaseUser: FirebaseUser, id: UUID): Template? {
        val options = ReadStreamOptions.get()
            .forwards()
            .fromStart()
//            .maxCount(10)

        val result = withContext(Dispatchers.IO) {
            client.readStream("checklist-template-$id", options).get()
        }
        if (result.events.isEmpty()) {
            return null
        }
        return result.events
            .map(::deserializeEvent)
            .fold(Template(id.toString(), "", "")) { acc, checklistTemplateEvent ->
                checklistTemplateEvent.applyTo(acc)
            }
    }

    @OptIn(ExperimentalSerializationApi::class)
    private fun deserializeEvent(event: ResolvedEvent): TemplateEventData {
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

            "addTemplateTask" -> {
                Json.decodeFromStream(TemplateEventData.AddTemplateTaskEventData.serializer(), eventData.inputStream())
            }

            "deleteTemplateTask" -> {
                Json.decodeFromStream(TemplateEventData.DeleteTemplateTaskData.serializer(), eventData.inputStream())
            }

            else -> error("Unknown eventType: $eventType")
        }
    }
}
