package tech.checkstory.checklist.data

import com.eventstore.dbclient.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import tech.checkstory.checklist.domain.event.ChecklistEvent
import tech.checkstory.data.EventRepository
import java.io.ByteArrayOutputStream
import java.util.*

class ChecklistEventRepository(client: EventStoreDBClient) : EventRepository<ChecklistEvent>(client) {

    @OptIn(ExperimentalSerializationApi::class)
    override fun save(event: ChecklistEvent) {
        val eventData = ByteArrayOutputStream().use {
            Json.encodeToStream(event, it)
            EventData
                .builderAsBinary(
                    event.eventId,
                    event.eventType,
                    it.toByteArray()
                ) // event type gets stored within dto - maybe not good!
                .build()
        }

        client.appendToStream("checklist-${event.checklistId}", eventData)
            .get()
    }

    override suspend fun getAll(): List<ChecklistEvent> {
        val result = withContext(Dispatchers.IO) {
            client.readAll(ReadAllOptions.get().fromStart().forwards()).await()
        }
        return result.events
            .mapNotNull(::deserializeEvent)
    }

    override suspend fun getEventsForAggregate(id: UUID): List<ChecklistEvent> {
        val result = withContext(Dispatchers.IO) {
            try {
                client.readStream("checklist-template-$id", readStreamOptions()).await().events
            } catch (exception: StreamNotFoundException) {
                listOf()
            }
        }
        return result.mapNotNull(::deserializeEvent)
    }

    @OptIn(ExperimentalSerializationApi::class)
    private fun deserializeEvent(event: ResolvedEvent): ChecklistEvent? {
        val eventType = event.event.eventType
        val eventData = event.event.eventData
        return when (eventType) {
            in knownChecklistEventTypes -> {
                Json.decodeFromStream(ChecklistEvent.serializer(), eventData.inputStream())
            }

            else -> null
        }
    }

    companion object {

        val knownChecklistEventTypes = setOf(
            "createChecklist",
            "editChecklistNotes",
            "changeTaskChecked",
            "deleteChecklist"
        )
    }
}