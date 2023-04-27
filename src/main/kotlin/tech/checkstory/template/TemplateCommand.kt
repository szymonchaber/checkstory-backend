package tech.checkstory.template

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface TemplateCommand : DomainCommand {

    val templateId: String

    @Serializable
    @SerialName("createTemplate")
    data class CreateTemplateCommand(
        override val templateId: String,
        override val eventId: String,
        override val timestamp: Long,
        override val userId: String
    ) : TemplateCommand

    @Serializable
    @SerialName("editTemplateTitle")
    data class EditTemplateTitleCommand(
        override val templateId: String,
        val newTitle: String,
        override val eventId: String,
        override val timestamp: Long,
        override val userId: String
    ) : TemplateCommand

    @Serializable
    @SerialName("editTemplateDescription")
    data class EditTemplateDescriptionCommand(
        override val templateId: String,
        val newDescription: String,
        override val eventId: String,
        override val timestamp: Long,
        override val userId: String
    ) : TemplateCommand

    @Serializable
    @SerialName("addTemplateTask")
    data class AddTemplateTaskCommand(
        override val templateId: String,
        val taskId: String,
        val parentTaskId: String?,
        override val eventId: String,
        override val timestamp: Long,
        override val userId: String
    ) : TemplateCommand

    @Serializable
    @SerialName("renameTemplateTask")
    data class RenameTemplateTaskCommand(
        override val templateId: String,
        val taskId: String,
        val newTitle: String,
        override val eventId: String,
        override val timestamp: Long,
        override val userId: String
    ) : TemplateCommand

    @Serializable
    @SerialName("deleteTemplateTask")
    data class DeleteTemplateTaskCommand(
        override val templateId: String,
        val taskId: String,
        override val eventId: String,
        override val timestamp: Long,
        override val userId: String
    ) : TemplateCommand

    @Serializable
    @SerialName("updateTemplateTasksPositions")
    data class UpdateTasksPositionsCommandDto(
        override val templateId: String,
        val taskIdToLocalPosition: Map<String, Long>,
        override val eventId: String,
        override val timestamp: Long,
        override val userId: String
    ) : TemplateCommand

    @Serializable
    @SerialName("moveTemplateTask")
    data class MoveTemplateTaskCommandDto(
        override val templateId: String,
        val taskId: String,
        val newParentTaskId: String?,
        override val eventId: String,
        override val timestamp: Long,
        override val userId: String
    ) : TemplateCommand
}