package tech.checkstory.checklist.data

import com.eventstore.dbclient.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import tech.checkstory.auth.user.User
import tech.checkstory.checklist.domain.ChecklistRepository
import tech.checkstory.checklist.domain.aggregate.Checklist
import tech.checkstory.checklist.domain.aggregate.ChecklistAggregate
import tech.checkstory.checklist.domain.event.ChecklistEvent
import tech.checkstory.domain.command.DtoUUID
import java.util.*

class EventBasedChecklistRepository(private val client: EventStoreDBClient) : ChecklistRepository {

    override suspend fun readAll(user: User): List<Checklist> {
        val result = withContext(Dispatchers.IO) {
            client.readAll(ReadAllOptions.get().fromStart().forwards()).await()
        }
        if (result.events.isEmpty()) {
            return listOf()
        }
        return result.events
            .asSequence()
            .mapNotNull(::deserializeEvent)
            .groupBy {
                it.checklistId
            }.mapNotNull { (id, events) ->
                ChecklistAggregate.fromHistory(id, events).toChecklistOrNull()
            }
            .filter {
                it.userId == user.userId
            }
            .filterNot {
                it.isDeleted
            }
            .toList()
    }

    override suspend fun read(user: User, id: UUID): Checklist? {
        return getById(user, id).toChecklistOrNull()
    }

    suspend fun getByIdOrNew(user: User, checklistId: DtoUUID): ChecklistAggregate {
        return getById(user, checklistId)
    }

    suspend fun getById(user: User, id: UUID): ChecklistAggregate {
        return getEventsForAggregate(id)
            .fold(new(id), ChecklistAggregate::applyEvent)
            .also {
                if (!it.isOwnedBy(user)) {
                    error("unauthorized access - 403")
                }
            }
    }

    fun new(id: UUID): ChecklistAggregate {
        return ChecklistAggregate(id)
    }

    private suspend fun getEventsForAggregate(id: UUID): List<ChecklistEvent> {
        val result = withContext(Dispatchers.IO) {
            try {
                client.readStream("checklist-template-$id", readStreamOptions()).await().events
            } catch (exception: StreamNotFoundException) {
                listOf()
            }
        }
        return result.mapNotNull(::deserializeEvent)
    }

    private fun readStreamOptions(): ReadStreamOptions {
        return ReadStreamOptions.get()
            .forwards()
            .fromStart()
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
}

val knownChecklistEventTypes = setOf(
    "createChecklist",
    "editChecklistNotes",
    "changeTaskChecked",
    "deleteChecklist"
)
