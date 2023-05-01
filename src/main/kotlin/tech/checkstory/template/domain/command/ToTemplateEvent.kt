package tech.checkstory.template.domain.command

import tech.checkstory.template.domain.event.TemplateEvent

fun TemplateCommand.toTemplateEvent(): TemplateEvent {
    return when (this) {
        is TemplateCommand.CreateTemplateCommand -> {
            TemplateEvent.CreateTemplateEvent(
                templateId,
                commandId,
                timestamp,
                userId,
            )
        }

        is TemplateCommand.EditTemplateTitleCommand -> {
            TemplateEvent.EditTemplateTitleEvent(
                templateId,
                newTitle,
                commandId,
                timestamp,
                userId,
            )
        }

        is TemplateCommand.EditTemplateDescriptionCommand -> {
            TemplateEvent.EditTemplateDescriptionEvent(
                templateId,
                newDescription,
                commandId,
                timestamp,
                userId,
            )
        }

        is TemplateCommand.AddTemplateTaskCommand -> {
            TemplateEvent.AddTemplateTaskEvent(
                templateId,
                taskId,
                parentTaskId,
                commandId,
                timestamp,
                userId,
            )
        }

        is TemplateCommand.RenameTemplateTaskCommand -> {
            TemplateEvent.RenameTemplateTaskEvent(
                templateId,
                taskId,
                newTitle,
                commandId,
                timestamp,
                userId,
            )
        }

        is TemplateCommand.DeleteTemplateTaskCommand -> {
            TemplateEvent.DeleteTemplateTaskEvent(
                templateId,
                taskId,
                commandId,
                timestamp,
                userId,
            )
        }

        is TemplateCommand.UpdateTasksPositionsCommandDto -> {
            TemplateEvent.UpdateTasksPositionsEvent(
                templateId,
                taskIdToLocalPosition,
                commandId,
                timestamp,
                userId,
            )
        }

        is TemplateCommand.MoveTemplateTaskCommandDto -> {
            TemplateEvent.MoveTemplateTaskEvent(
                templateId,
                taskId,
                newParentTaskId,
                commandId,
                timestamp,
                userId
            )
        }

        is TemplateCommand.AddOrUpdateTemplateReminderCommandDto -> {
            TemplateEvent.AddOrUpdateReminderEvent(
                templateId,
                reminder,
                commandId,
                timestamp,
                userId,
            )
        }

        is TemplateCommand.DeleteTemplateReminderCommandDto -> {
            TemplateEvent.DeleteReminderEvent(
                templateId,
                reminderId,
                commandId,
                timestamp,
                userId,
            )
        }

        is TemplateCommand.DeleteTemplateCommandDto -> {
            TemplateEvent.DeleteTemplateEvent(
                templateId,
                commandId,
                timestamp,
                userId,
            )
        }
    }
}