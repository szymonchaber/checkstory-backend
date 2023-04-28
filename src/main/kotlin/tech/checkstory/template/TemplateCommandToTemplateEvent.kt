package tech.checkstory.template

fun TemplateCommand.toTemplateEvent(): TemplateEvent {
    return when (this) {
        is TemplateCommand.CreateTemplateCommand -> {
            TemplateEvent.CreateTemplateEvent(
                templateId,
                eventId,
                timestamp,
                userId,
            )
        }

        is TemplateCommand.EditTemplateTitleCommand -> {
            TemplateEvent.EditTemplateTitleEvent(
                templateId,
                newTitle,
                eventId,
                timestamp,
                userId,
            )
        }

        is TemplateCommand.EditTemplateDescriptionCommand -> {
            TemplateEvent.EditTemplateDescriptionEvent(
                templateId,
                newDescription,
                eventId,
                timestamp,
                userId,
            )
        }

        is TemplateCommand.AddTemplateTaskCommand -> {
            TemplateEvent.AddTemplateTaskEvent(
                templateId,
                taskId,
                parentTaskId,
                eventId,
                timestamp,
                userId,
            )
        }

        is TemplateCommand.RenameTemplateTaskCommand -> {
            TemplateEvent.RenameTemplateTaskEvent(
                templateId,
                taskId,
                newTitle,
                eventId,
                timestamp,
                userId,
            )
        }

        is TemplateCommand.DeleteTemplateTaskCommand -> {
            TemplateEvent.DeleteTemplateTaskEvent(
                templateId,
                taskId,
                eventId,
                timestamp,
                userId,
            )
        }

        is TemplateCommand.UpdateTasksPositionsCommandDto -> {
            TemplateEvent.UpdateTasksPositionsEvent(
                templateId,
                taskIdToLocalPosition,
                eventId,
                timestamp,
                userId,
            )
        }

        is TemplateCommand.MoveTemplateTaskCommandDto -> {
            TemplateEvent.MoveTemplateTaskEvent(
                templateId,
                taskId,
                newParentTaskId,
                eventId,
                timestamp,
                userId
            )
        }

        is TemplateCommand.AddOrUpdateTemplateReminderCommandDto -> {
            TemplateEvent.AddOrUpdateReminderEvent(
                templateId,
                reminder,
                eventId,
                timestamp,
                userId,
            )
        }

        is TemplateCommand.DeleteTemplateReminderCommandDto -> {
            TemplateEvent.DeleteReminderEvent(
                templateId,
                reminderId,
                eventId,
                timestamp,
                userId,
            )
        }
    }
}