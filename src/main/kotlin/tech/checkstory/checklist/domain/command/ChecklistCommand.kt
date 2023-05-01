package tech.checkstory.checklist.domain.command

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import tech.checkstory.checklist.domain.aggregate.Task
import tech.checkstory.checklist.domain.event.ChecklistEvent
import tech.checkstory.template.DomainCommand
import tech.checkstory.template.DtoUUID

@Serializable
sealed interface ChecklistCommand : DomainCommand {

    val checklistId: DtoUUID

    @Serializable
    @SerialName("createChecklist")
    data class CreateChecklistCommand(
        override val checklistId: DtoUUID,
        val templateId: DtoUUID,
        val title: String,
        val description: String,
        val tasks: List<Task>,
        override val eventId: DtoUUID,
        override val timestamp: Long,
        override val userId: String
    ) : ChecklistCommand

    @Serializable
    @SerialName("editChecklistNotes")
    data class EditChecklistNotesCommand(
        override val checklistId: DtoUUID,
        val newNotes: String,
        override val eventId: DtoUUID,
        override val timestamp: Long,
        override val userId: String
    ) : ChecklistCommand

    @Serializable
    @SerialName("changeTaskChecked")
    data class ChangeTaskCheckedCommand(
        override val checklistId: DtoUUID,
        val taskId: DtoUUID,
        val isChecked: Boolean,
        override val eventId: DtoUUID,
        override val timestamp: Long,
        override val userId: String
    ) : ChecklistCommand

    @Serializable
    @SerialName("deleteChecklist")
    data class DeleteChecklistCommand(
        override val checklistId: DtoUUID,
        override val eventId: DtoUUID,
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