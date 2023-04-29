package tech.checkstory.checklist

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import tech.checkstory.template.DomainCommand

@Serializable
sealed interface ChecklistCommand : DomainCommand {

    val checklistId: String

    @Serializable
    @SerialName("createChecklist")
    data class CreateChecklistCommand(
        override val checklistId: String,
        val templateId: String,
        val title: String,
        val description: String,
        val tasks: List<Task>,
        override val eventId: String,
        override val timestamp: Long,
        override val userId: String
    ) : ChecklistCommand

    @Serializable
    @SerialName("editChecklistNotes")
    data class EditChecklistNotesCommand(
        override val checklistId: String,
        val newNotes: String,
        override val eventId: String,
        override val timestamp: Long,
        override val userId: String
    ) : ChecklistCommand

    @Serializable
    @SerialName("changeTaskChecked")
    data class ChangeTaskCheckedCommand(
        override val checklistId: String,
        val taskId: String,
        val isChecked: Boolean,
        override val eventId: String,
        override val timestamp: Long,
        override val userId: String
    ) : ChecklistCommand

    @Serializable
    @SerialName("deleteChecklist")
    data class DeleteChecklistCommand(
        override val checklistId: String,
        override val eventId: String,
        override val timestamp: Long,
        override val userId: String
    ) : ChecklistCommand
}

fun ChecklistCommand.toChecklistEvent(): ChecklistEvent {
    return when (this) {
        is ChecklistCommand.CreateChecklistCommand -> {
            ChecklistEvent.CreateChecklistEvent(
                checklistId = checklistId,
                templateId = templateId,
                title = title,
                description = description,
                tasks = tasks,
                eventId = eventId,
                timestamp = timestamp,
                userId = userId
            )
        }

        is ChecklistCommand.EditChecklistNotesCommand -> {
            ChecklistEvent.EditChecklistNotesEvent(
                checklistId,
                newNotes,
                eventId,
                timestamp,
                userId
            )
        }

        is ChecklistCommand.ChangeTaskCheckedCommand -> {
            ChecklistEvent.ChangeTaskCheckedEvent(
                checklistId,
                taskId,
                isChecked,
                eventId,
                timestamp,
                userId
            )
        }

        is ChecklistCommand.DeleteChecklistCommand -> {
            ChecklistEvent.DeleteChecklistEvent(
                checklistId,
                eventId,
                timestamp,
                userId
            )
        }
    }
}