package tech.checkstory.checklist

import com.eventstore.dbclient.EventStoreDBClient
import com.eventstore.dbclient.ReadAllOptions
import com.eventstore.dbclient.ReadStreamOptions
import com.eventstore.dbclient.ResolvedEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import tech.checkstory.firebase.user.FirebaseUser
import tech.checkstory.template.DomainEvent
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
            .mapNotNull(::deserializeEvent)
            .groupBy {
                it.checklistId
            }.map { (id, events) ->
                require(events.first() is ChecklistEvent.CreateChecklistEvent) {
                    "First event must be a CreateChecklistEvent. Was: ${events.first()}"
                }
                events.fold(Checklist(id, "", "", "", "", "", listOf())) { acc, event ->
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
                    id = id.toString(),
                    userId = "",
                    title = "",
                    notes = "",
                    templateId = "",
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

interface ChecklistRepository {

    suspend fun readAll(firebaseUser: FirebaseUser): List<Checklist>
    suspend fun read(firebaseUser: FirebaseUser, id: UUID): Checklist?
}

@Serializable
data class Checklist(
    val id: String,
    val userId: String,
    val templateId: String,
    val title: String,
    val description: String,
    val notes: String,
    val tasks: List<Task> = listOf(),
    val isDeleted: Boolean = false
)

@Serializable
data class Task(
    val id: String,
    val checklistId: String,
    val title: String,
    val sortPosition: Long,
    val isChecked: Boolean,
    val children: List<Task> = listOf()
) {

    fun withUpdatedIsCheckedRecursive(id: String, isChecked: Boolean): Task {
        return if (this.id == id) {
            copy(isChecked = isChecked)
        } else {
            copy(children = children.map { it.withUpdatedIsCheckedRecursive(id, isChecked) })
        }
    }

}

@Serializable
sealed interface ChecklistEvent : DomainEvent {

    val checklistId: String

    fun applyTo(checklist: Checklist): Checklist

    @Serializable
    @SerialName("createChecklist")
    data class CreateChecklistEvent(
        override val checklistId: String,
        val templateId: String,
        val title: String,
        val description: String,
        val tasks: List<Task>,
        override val eventId: String,
        override val timestamp: Long,
        override val userId: String
    ) : ChecklistEvent {

        override val eventType: String = "createChecklist"

        override fun applyTo(checklist: Checklist): Checklist {
            return checklist.copy(
                id = checklistId,
                templateId = templateId,
                title = title,
                description = description,
                tasks = tasks,
                userId = userId
            )
        }
    }

    @Serializable
    @SerialName("editChecklistNotes")
    data class EditChecklistNotesEvent(
        override val checklistId: String,
        val newNotes: String,
        override val eventId: String,
        override val timestamp: Long,
        override val userId: String
    ) : ChecklistEvent {

        override val eventType: String = "editChecklistNotes"

        override fun applyTo(checklist: Checklist): Checklist {
            require(checklist.userId == userId)
            return checklist.copy(notes = newNotes)
        }
    }

    @Serializable
    @SerialName("changeTaskChecked")
    data class ChangeTaskCheckedEvent(
        override val checklistId: String,
        val taskId: String,
        val isChecked: Boolean,
        override val eventId: String,
        override val timestamp: Long,
        override val userId: String
    ) : ChecklistEvent {

        override val eventType: String = "changeTaskChecked"

        override fun applyTo(checklist: Checklist): Checklist {
            require(checklist.userId == userId)
            val newTasks = checklist.tasks.map {
                it.withUpdatedIsCheckedRecursive(taskId, isChecked)
            }
            return checklist.copy(tasks = newTasks)
        }
    }

    @Serializable
    @SerialName("deleteChecklist")
    data class DeleteChecklistEvent(
        override val checklistId: String,
        override val eventId: String,
        override val timestamp: Long,
        override val userId: String
    ) : ChecklistEvent {

        override val eventType: String
            get() = "deleteChecklist"

        override fun applyTo(checklist: Checklist): Checklist {
            return checklist.copy(isDeleted = true)
        }
    }
}

val knownChecklistEventTypes = setOf(
    "createChecklist",
    "editChecklistNotes",
    "changeTaskChecked",
    "deleteChecklist"
)
