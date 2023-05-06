package tech.checkstory.checklist.domain.event

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import tech.checkstory.checklist.domain.aggregate.Task
import tech.checkstory.domain.command.DtoUUID
import tech.checkstory.domain.event.DomainEvent

@Serializable
sealed interface ChecklistEvent : DomainEvent {

    val checklistId: DtoUUID

    @Serializable
    @SerialName("createChecklist")
    data class CreateChecklistEvent(
        override val checklistId: DtoUUID,
        val templateId: DtoUUID,
        val title: String,
        val description: String,
        val tasks: List<Task>,
        override val eventId: DtoUUID,
        override val timestamp: Long,
        override val userId: String
    ) : ChecklistEvent {

        override val eventType: String = "createChecklist"
    }

    @Serializable
    @SerialName("editChecklistNotes")
    data class EditChecklistNotesEvent(
        override val checklistId: DtoUUID,
        val newNotes: String,
        override val eventId: DtoUUID,
        override val timestamp: Long,
        override val userId: String
    ) : ChecklistEvent {

        override val eventType: String = "editChecklistNotes"
    }

    @Serializable
    @SerialName("changeTaskChecked")
    data class ChangeTaskCheckedEvent(
        override val checklistId: DtoUUID,
        val taskId: DtoUUID,
        val isChecked: Boolean,
        override val eventId: DtoUUID,
        override val timestamp: Long,
        override val userId: String
    ) : ChecklistEvent {

        override val eventType: String = "changeTaskChecked"
    }

    @Serializable
    @SerialName("deleteChecklist")
    data class DeleteChecklistEvent(
        override val checklistId: DtoUUID,
        override val eventId: DtoUUID,
        override val timestamp: Long,
        override val userId: String
    ) : ChecklistEvent {

        override val eventType: String
            get() = "deleteChecklist"
    }
}
