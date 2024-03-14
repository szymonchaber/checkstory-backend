package tech.checkstory.template.data

import tech.checkstory.core.domain.DtoUUID
import tech.checkstory.core.domain.auth.User
import tech.checkstory.template.domain.aggregate.Template
import tech.checkstory.template.domain.command.ExistingTemplateTaskDto
import tech.checkstory.template.domain.command.TemplateCommand
import tech.checkstory.template.domain.event.TemplateEvent
import java.util.*

class TemplateAggregate(val id: UUID) {

    var templateState: TemplateState = TemplateState.NonExistent

    fun applyCommands(user: User, commands: List<TemplateCommand>): List<TemplateEvent> {
        val state = templateState
        require(state is TemplateState.NonExistent || user.userId == (state as TemplateState.Existing).userId) {
            "command users must equal firebase users"
        }
        return commands.mapNotNull {
            it.toTemplateEvent(user.userId)
        }
    }

    private fun TemplateCommand.toTemplateEvent(userId: String): TemplateEvent? {
        return when (this) {
            is TemplateCommand.CreateTemplateCommand -> {
                templateState = TemplateState.Existing(
                    userId = userId,
                    id = templateId,
                    name = existingTemplateData?.title.orEmpty(),
                    description = existingTemplateData?.title.orEmpty(),
                    tasks = listOf(),
                    reminders = listOf(),
                    createdAt = timestamp
                )
                TemplateEvent.CreateTemplateEvent(
                    aggregateId = templateId,
                    timestamp = timestamp,
                    userId = userId,
                    existingTemplateData = existingTemplateData
                )
            }

            is TemplateCommand.EditTemplateTitleCommand -> {
                templateState = (templateState as TemplateState.Existing).copy(name = newTitle)
                null
            }

            is TemplateCommand.EditTemplateDescriptionCommand -> {
//                templateState = (templateState as TemplateState.Existing).copy(description = newDescription)
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

            is TemplateCommand.UpdateTasksPositionsCommand -> {
                TemplateEvent.UpdateTasksPositionsEvent(
                    taskIdToLocalPosition = taskIdToLocalPosition,
                    aggregateId = templateId,
                    timestamp = timestamp,
                )
            }

            is TemplateCommand.MoveTemplateTaskCommand -> {
                TemplateEvent.MoveTemplateTaskEvent(
                    taskId = taskId,
                    newParentTaskId = newParentTaskId,
                    aggregateId = templateId,
                    timestamp = timestamp,
                )
            }

            is TemplateCommand.AddOrUpdateTemplateReminderCommand -> {
                TemplateEvent.AddOrUpdateReminderEvent(
                    reminder = reminder,
                    aggregateId = templateId,
                    timestamp = timestamp,
                )
            }

            is TemplateCommand.DeleteTemplateReminderCommand -> {
                TemplateEvent.DeleteReminderEvent(
                    reminderId = reminderId,
                    aggregateId = templateId,
                    timestamp = timestamp,
                )
            }

            is TemplateCommand.DeleteTemplateCommand -> {
                TemplateEvent.DeleteTemplateEvent(
                    aggregateId = templateId,
                    timestamp = timestamp,
                )
            }
        }
    }

    fun toAggregateTemplateTask(
        existingTask: ExistingTemplateTaskDto,
        templateId: DtoUUID
    ): AggregateTemplateTask {
        return AggregateTemplateTask(
            id = existingTask.id,
            templateId = templateId,
            title = existingTask.title,
            sortPosition = existingTask.sortPosition,
            parentId = existingTask.parentId
        )
    }

    // flatten dto tasks recursively
    private fun flattenTasks(tasks: List<ExistingTemplateTaskDto>): List<AggregateTemplateTask> {
        return tasks.flatMap { task ->
            listOf(
                toAggregateTemplateTask(task, task.id),
                *flattenTasks(task.children).toTypedArray()
            )
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
