package tech.checkstory.checklist.domain

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import tech.checkstory.checklist.domain.aggregate.Task
import tech.checkstory.core.domain.DomainCommand
import tech.checkstory.core.domain.DtoUUID

@Serializable
sealed interface ChecklistCommand : DomainCommand {

    val checklistId: DtoUUID

    @Serializable
    @SerialName("createChecklist")
    data class CreateChecklistCommand(
        override val checklistId: DtoUUID,
        val templateId: DtoUUID,
        val tasks: List<Task>,
        val notes: String?,
        override val commandId: DtoUUID,
        override val timestamp: Instant
    ) : ChecklistCommand

    @Serializable
    @SerialName("editChecklistNotes")
    data class EditChecklistNotesCommand(
        override val checklistId: DtoUUID,
        val newNotes: String,
        override val commandId: DtoUUID,
        override val timestamp: Instant
    ) : ChecklistCommand

    @Serializable
    @SerialName("changeTaskChecked")
    data class ChangeTaskCheckedCommand(
        override val checklistId: DtoUUID,
        val taskId: DtoUUID,
        val isChecked: Boolean,
        override val commandId: DtoUUID,
        override val timestamp: Instant
    ) : ChecklistCommand

    @Serializable
    @SerialName("deleteChecklist")
    data class DeleteChecklistCommand(
        override val checklistId: DtoUUID,
        override val commandId: DtoUUID,
        override val timestamp: Instant
    ) : ChecklistCommand
}
