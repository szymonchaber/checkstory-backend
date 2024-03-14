package tech.checkstory.template.data

import tech.checkstory.core.domain.DtoUUID
import tech.checkstory.core.domain.auth.User
import tech.checkstory.template.domain.aggregate.Template
import tech.checkstory.template.domain.aggregate.TemplateTask
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
                templateState = (templateState as TemplateState.Existing).copy(description = newDescription)
                null
            }

            is TemplateCommand.AddTemplateTaskCommand -> {
                templateState = (templateState as TemplateState.Existing).copy(
                    tasks = (templateState as TemplateState.Existing).tasks +
                            TemplateTask(
                                id = taskId,
                                title = "",
                                sortPosition = 0,
                                parentId = parentTaskId,
                                templateId = templateId,
                                children = listOf()
                            )
                )
                null
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
                templateState = (templateState as TemplateState.Existing).copy(
                    reminders = (templateState as TemplateState.Existing).reminders + AggregateReminder(
                        reminder = reminder.toReminder(),
                        isDeleted = false
                    )
                )
                null
            }

            is TemplateCommand.DeleteTemplateReminderCommand -> {
                templateState = (templateState as TemplateState.Existing).copy(
                    reminders = (templateState as TemplateState.Existing).reminders.map {
                        if (it.reminder.id == reminderId) {
                            it.copy(isDeleted = true)
                        } else {
                            it
                        }
                    }
                )
                null
            }

            is TemplateCommand.DeleteTemplateCommand -> {
                templateState = (templateState as TemplateState.Existing).copy(isDeleted = true)
                null
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
                    reminders = template.reminders.map { AggregateReminder(it, false) },
                    createdAt = template.createdAt
                )
            }
        }
    }
}
