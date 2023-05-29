package tech.checkstory.template.data

import com.eventstore.dbclient.EventStoreDBClient
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import tech.checkstory.data.EventRepository
import tech.checkstory.template.domain.event.TemplateEvent
import java.io.ByteArrayOutputStream

class TemplateEventRepository(client: EventStoreDBClient) : EventRepository<TemplateEvent>(client) {

    override val streamNamePrefix = "checklist-template"

    override val knownEventTypes = setOf(
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

    @OptIn(ExperimentalSerializationApi::class)
    override fun encode(event: TemplateEvent, outputStream: ByteArrayOutputStream) {
        Json.encodeToStream(event, outputStream)
    }

    @OptIn(ExperimentalSerializationApi::class)
    override fun decode(eventData: ByteArray): TemplateEvent {
        return Json.decodeFromStream(TemplateEvent.serializer(), eventData.inputStream())
    }
}