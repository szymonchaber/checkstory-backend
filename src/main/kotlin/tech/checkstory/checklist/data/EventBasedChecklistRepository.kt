package tech.checkstory.checklist.data

import com.eventstore.dbclient.EventStoreDBClient
import com.eventstore.dbclient.ReadAllOptions
import com.eventstore.dbclient.ReadStreamOptions
import com.eventstore.dbclient.ResolvedEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import tech.checkstory.checklist.domain.ChecklistRepository
import tech.checkstory.checklist.domain.aggregate.Checklist
import tech.checkstory.checklist.domain.aggregate.ChecklistAggregate
import tech.checkstory.checklist.domain.event.ChecklistEvent
import tech.checkstory.firebase.user.FirebaseUser
import tech.checkstory.template.extensions.requireElementOfType
import java.util.*

class EventBasedChecklistRepository(private val client: EventStoreDBClient) : ChecklistRepository {

    override suspend fun readAll(firebaseUser: FirebaseUser): List<Checklist> {
        val result = withContext(Dispatchers.IO) {
            client.readAll(ReadAllOptions.get().fromStart().forwards()).get()
        }
        if (result.events.isEmpty()) {
            return listOf()
        }
        return result.events
            .asSequence()
            .mapNotNull(::deserializeEvent)
            .groupBy {
                it.checklistId
            }.map { (_, events) ->
                val (createChecklist, otherEvents) = events.requireElementOfType<ChecklistEvent, ChecklistEvent.CreateChecklistEvent>()
                ChecklistAggregate(createChecklist, otherEvents).checklist
            }
            .filter {
                it.userId == firebaseUser.userId
            }
            .filterNot {
                it.isDeleted
            }
            .toList()
    }

    override suspend fun read(firebaseUser: FirebaseUser, id: UUID): Checklist? {
        val result = withContext(Dispatchers.IO) {
            client.readStream("checklist-template-$id", readStreamOptions()).get()
        }
        if (result.events.isEmpty()) {
            return null
        }
        return result.events
            .mapNotNull(::deserializeEvent)
            .fold(
                Checklist(
                    id = id,
                    userId = "",
                    title = "",
                    notes = "",
                    templateId = UUID.randomUUID(),
                    description = "",
                    tasks = listOf()
                )
            ) { acc, checklistChecklistEvent ->
                checklistChecklistEvent.applyTo(acc)
            }
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
