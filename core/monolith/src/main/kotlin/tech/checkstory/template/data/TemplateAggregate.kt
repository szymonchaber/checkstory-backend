package tech.checkstory.template.data

import tech.checkstory.core.domain.auth.User
import tech.checkstory.template.domain.aggregate.Template
import tech.checkstory.template.domain.command.TemplateCommand
import tech.checkstory.template.domain.event.TemplateEvent
import java.util.*

class TemplateAggregate(val id: UUID) {

    private var templateState: TemplateState = TemplateState.NonExistent

    fun toTemplateOrNull(): Template? {
        return when (val state = templateState) {
            is TemplateState.Existing -> state.toTemplate()
            TemplateState.NonExistent -> null
        }
    }

    fun applyCommands(user: User, commands: List<TemplateCommand>): List<TemplateEvent> {
        val state = templateState
        require(state is TemplateState.NonExistent || user.userId == (state as TemplateState.Existing).userId) {
            "command users must equal firebase users"
        }
        return commands.map {
            it.toTemplateEvent(user.userId)
        }
    }

    private fun TemplateCommand.toTemplateEvent(userId: String): TemplateEvent {
        return when (this) {
            is TemplateCommand.CreateTemplateCommand -> {
                TemplateEvent.CreateTemplateEvent(
                    aggregateId = templateId,
                    timestamp = timestamp,
                    userId = userId,
                    existingTemplateData = existingTemplateData
                )
            }

            is TemplateCommand.EditTemplateTitleCommand -> {
                TemplateEvent.EditTemplateTitleEvent(
                    newTitle = newTitle,
                    aggregateId = templateId,
                    timestamp = timestamp,
                )
            }

            is TemplateCommand.EditTemplateDescriptionCommand -> {
                TemplateEvent.EditTemplateDescriptionEvent(
                    newDescription = newDescription,
                    aggregateId = templateId,
                    timestamp = timestamp,
                )
            }

            is TemplateCommand.AddTemplateTaskCommand -> {
                TemplateEvent.AddTemplateTaskEvent(
                    taskId = taskId,
                    parentTaskId = parentTaskId,
                    aggregateId = templateId,
                    timestamp = timestamp,
                )
            }

            is TemplateCommand.RenameTemplateTaskCommand -> {
                TemplateEvent.RenameTemplateTaskEvent(
                    taskId = taskId,
                    newTitle = newTitle,
                    aggregateId = templateId,
                    timestamp = timestamp,
                )
            }

            is TemplateCommand.DeleteTemplateTaskCommand -> {
                TemplateEvent.DeleteTemplateTaskEvent(
                    taskId = taskId,
                    aggregateId = templateId,
                    timestamp = timestamp,
                )
            }

            is TemplateCommand.UpdateTasksPositionsCommandDto -> {
                TemplateEvent.UpdateTasksPositionsEvent(
                    taskIdToLocalPosition = taskIdToLocalPosition,
                    aggregateId = templateId,
                    timestamp = timestamp,
                )
            }

            is TemplateCommand.MoveTemplateTaskCommandDto -> {
                TemplateEvent.MoveTemplateTaskEvent(
                    taskId = taskId,
                    newParentTaskId = newParentTaskId,
                    aggregateId = templateId,
                    timestamp = timestamp,
                )
            }

            is TemplateCommand.AddOrUpdateTemplateReminderCommandDto -> {
                TemplateEvent.AddOrUpdateReminderEvent(
                    reminder = reminder,
                    aggregateId = templateId,
                    timestamp = timestamp,
                )
            }

            is TemplateCommand.DeleteTemplateReminderCommandDto -> {
                TemplateEvent.DeleteReminderEvent(
                    reminderId = reminderId,
                    aggregateId = templateId,
                    timestamp = timestamp,
                )
            }

            is TemplateCommand.DeleteTemplateCommandDto -> {
                TemplateEvent.DeleteTemplateEvent(
                    aggregateId = templateId,
                    timestamp = timestamp,
                )
            }
        }
    }

    companion object {

        fun fromTemplate(template: Template): TemplateAggregate {
            return TemplateAggregate(template.id).apply {
                templateState = TemplateState.Existing(
                    userId = template.userId,
                    id = template.id,
                    name = template.name,
                    description = template.description,
                    tasks = template.tasks,
                    reminders = template.reminders,
                    createdAt = template.createdAt
                )
            }
        }
    }
}
