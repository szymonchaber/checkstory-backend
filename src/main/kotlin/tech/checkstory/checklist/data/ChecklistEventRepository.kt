package tech.checkstory.checklist.data

import com.eventstore.dbclient.EventStoreDBClient
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import tech.checkstory.checklist.domain.event.ChecklistEvent
import tech.checkstory.data.EventRepository
import java.io.ByteArrayOutputStream

class ChecklistEventRepository(client: EventStoreDBClient) : EventRepository<ChecklistEvent>(client) {

    override val streamNamePrefix = "checklist"

    override val knownEventTypes = setOf(
        "createChecklist",
        "editChecklistNotes",
        "changeTaskChecked",
        "deleteChecklist"
    )

    override suspend fun save(event: ChecklistEvent) {
        val id = event.checklistId
        save(event, id)
    }

    @OptIn(ExperimentalSerializationApi::class)
    override fun encode(event: ChecklistEvent, outputStream: ByteArrayOutputStream) {
        Json.encodeToStream(event, outputStream)
    }

    @OptIn(ExperimentalSerializationApi::class)
    override fun decode(eventData: ByteArray): ChecklistEvent {
        return Json.decodeFromStream(ChecklistEvent.serializer(), eventData.inputStream())
    }
}
