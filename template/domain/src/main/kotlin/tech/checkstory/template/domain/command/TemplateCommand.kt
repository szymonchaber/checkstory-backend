package tech.checkstory.template.domain.command

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import tech.checkstory.core.domain.DtoUUID
import tech.checkstory.domain.command.DomainCommand

@Serializable
sealed interface TemplateCommand : DomainCommand {

    val templateId: DtoUUID

    @Serializable
    @SerialName("createTemplate")
    data class CreateTemplateCommand(
        override val templateId: DtoUUID,
        override val commandId: DtoUUID,
        override val timestamp: Instant,
        val existingTemplateData: ExistingTemplateDataDto?
    ) : TemplateCommand

    @Serializable
    @SerialName("editTemplateTitle")
    data class EditTemplateTitleCommand(
        override val templateId: DtoUUID,
        val newTitle: String,
        override val commandId: DtoUUID,
        override val timestamp: Instant
    ) : TemplateCommand

    @Serializable
    @SerialName("editTemplateDescription")
    data class EditTemplateDescriptionCommand(
        override val templateId: DtoUUID,
        val newDescription: String,
        override val commandId: DtoUUID,
        override val timestamp: Instant
    ) : TemplateCommand

    @Serializable
    @SerialName("addTemplateTask")
    data class AddTemplateTaskCommand(
        override val templateId: DtoUUID,
        val taskId: DtoUUID,
        val parentTaskId: DtoUUID?,
        override val commandId: DtoUUID,
        override val timestamp: Instant
    ) : TemplateCommand

    @Serializable
    @SerialName("renameTemplateTask")
    data class RenameTemplateTaskCommand(
        override val templateId: DtoUUID,
        val taskId: DtoUUID,
        val newTitle: String,
        override val commandId: DtoUUID,
        override val timestamp: Instant
    ) : TemplateCommand

    @Serializable
    @SerialName("deleteTemplateTask")
    data class DeleteTemplateTaskCommand(
        override val templateId: DtoUUID,
        val taskId: DtoUUID,
        override val commandId: DtoUUID,
        override val timestamp: Instant
    ) : TemplateCommand

    @Serializable
    @SerialName("updateTemplateTasksPositions")
    data class UpdateTasksPositionsCommand(
        override val templateId: DtoUUID,
        val taskIdToLocalPosition: Map<DtoUUID, Long>,
        override val commandId: DtoUUID,
        override val timestamp: Instant
    ) : TemplateCommand

    @Serializable
    @SerialName("moveTemplateTask")
    data class MoveTemplateTaskCommand(
        override val templateId: DtoUUID,
        val taskId: DtoUUID,
        val newParentTaskId: DtoUUID?,
        override val commandId: DtoUUID,
        override val timestamp: Instant
    ) : TemplateCommand

    @Serializable
    @SerialName("addOrUpdateTemplateReminder")
    data class AddOrUpdateTemplateReminderCommand(
        override val templateId: DtoUUID,
        val reminder: ReminderDto,
        override val commandId: DtoUUID,
        override val timestamp: Instant
    ) : TemplateCommand

    @Serializable
    @SerialName("deleteTemplateReminder")
    data class DeleteTemplateReminderCommand(
        override val templateId: DtoUUID,
        val reminderId: DtoUUID,
        override val commandId: DtoUUID,
        override val timestamp: Instant
    ) : TemplateCommand

    @Serializable
    @SerialName("deleteTemplate")
    data class DeleteTemplateCommand(
        override val templateId: DtoUUID,
        override val commandId: DtoUUID,
        override val timestamp: Instant
    ) : TemplateCommand
}
