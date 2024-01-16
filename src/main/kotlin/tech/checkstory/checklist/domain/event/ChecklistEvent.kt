package tech.checkstory.checklist.domain.event

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import tech.checkstory.checklist.domain.aggregate.Task
import tech.checkstory.core.domain.DtoUUID
import tech.checkstory.domain.event.DomainEvent

@Serializable
sealed interface ChecklistEvent : DomainEvent {

    @Serializable
    @SerialName("createChecklist")
    data class CreateChecklistEvent(
        val templateId: DtoUUID,
        val title: String,
        val description: String,
        val tasks: List<Task>,
        val userId: String,
        val notes: String?,
        override val aggregateId: DtoUUID,
        override val eventId: DtoUUID,
        override val timestamp: Instant,
    ) : ChecklistEvent {

        override val eventType: String = "createChecklist"
    }

    @Serializable
    @SerialName("editChecklistNotes")
    data class EditChecklistNotesEvent(
        val newNotes: String,
        override val aggregateId: DtoUUID,
        override val eventId: DtoUUID,
        override val timestamp: Instant,
    ) : ChecklistEvent {

        override val eventType: String = "editChecklistNotes"
    }

    @Serializable
    @SerialName("changeTaskChecked")
    data class ChangeTaskCheckedEvent(
        val taskId: DtoUUID,
        val isChecked: Boolean,
        override val aggregateId: DtoUUID,
        override val eventId: DtoUUID,
        override val timestamp: Instant,
    ) : ChecklistEvent {

        override val eventType: String = "changeTaskChecked"
    }

    @Serializable
    @SerialName("deleteChecklist")
    data class DeleteChecklistEvent(
        override val aggregateId: DtoUUID,
        override val eventId: DtoUUID,
        override val timestamp: Instant,
    ) : ChecklistEvent {

        override val eventType: String
            get() = "deleteChecklist"
    }
}
