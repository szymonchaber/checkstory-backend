package tech.checkstory.template.domain

import tech.checkstory.core.domain.auth.User
import tech.checkstory.template.domain.aggregate.Template
import tech.checkstory.template.domain.aggregate.TemplateTask
import tech.checkstory.template.domain.command.TemplateCommand
import java.util.*

class TemplateAggregate(val id: UUID) {

    private var templateState: TemplateState = TemplateState.NonExistent

    fun applyCommands(user: User, commands: List<TemplateCommand>): TemplateState {
        val state = templateState
        require(state is TemplateState.NonExistent || user.userId == (state as TemplateState.Existing).userId) {
            "command users must equal firebase users"
        }
        commands.forEach {
            it.apply(user.userId)
        }
        return templateState
    }

    private fun TemplateCommand.apply(userId: String) {
        when (this) {
            is TemplateCommand.CreateTemplateCommand -> {
                templateState = TemplateState.Existing(
                    userId = userId,
                    id = templateId,
                    name = existingTemplateData?.title.orEmpty(),
                    description = existingTemplateData?.description.orEmpty(),
                    tasks = existingTemplateData?.tasks?.map { it.toTemplateTask(templateId) }?.flatten().orEmpty(),
                    reminders = existingTemplateData?.reminders?.map { AggregateReminder(it.toReminder(), false) }
                        .orEmpty(),
                    createdAt = timestamp
                )
            }

            is TemplateCommand.EditTemplateTitleCommand -> {
                templateState = (templateState as TemplateState.Existing).copy(name = newTitle)
            }

            is TemplateCommand.EditTemplateDescriptionCommand -> {
                templateState = (templateState as TemplateState.Existing).copy(description = newDescription)
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
            }

            is TemplateCommand.RenameTemplateTaskCommand -> {
                templateState = (templateState as TemplateState.Existing).copy(
                    tasks = (templateState as TemplateState.Existing).tasks.map {
                        if (it.id == taskId) {
                            it.copy(title = newTitle)
                        } else {
                            it
                        }
                    }
                )
            }

            is TemplateCommand.DeleteTemplateTaskCommand -> {
                templateState = (templateState as TemplateState.Existing).copy(
                    tasks = (templateState as TemplateState.Existing).tasks.map {
                        if (it.id == taskId) {
                            it.copy(isDeleted = true)
                        } else {
                            it
                        }
                    }
                )
            }

            is TemplateCommand.UpdateTasksPositionsCommand -> {
                templateState = (templateState as TemplateState.Existing).copy(
                    tasks = (templateState as TemplateState.Existing).tasks.map {
                        it.copy(sortPosition = taskIdToLocalPosition[it.id] ?: it.sortPosition)
                    }
                )
            }

            is TemplateCommand.MoveTemplateTaskCommand -> {
                templateState = (templateState as TemplateState.Existing).copy(
                    tasks = (templateState as TemplateState.Existing).tasks.map {
                        if (it.id == taskId) {
                            it.copy(parentId = newParentTaskId)
                        } else {
                            it
                        }
                    }
                )
            }

            is TemplateCommand.AddOrUpdateTemplateReminderCommand -> {
                templateState = (templateState as TemplateState.Existing).copy(
                    reminders = (templateState as TemplateState.Existing).reminders + AggregateReminder(
                        reminder = reminder.toReminder(),
                        isDeleted = false
                    )
                )
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
            }

            is TemplateCommand.DeleteTemplateCommand -> {
                templateState = (templateState as TemplateState.Existing).copy(isDeleted = true)
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
                    reminders = template.reminders.map { AggregateReminder(it, false) },
                    createdAt = template.createdAt
                )
            }
        }

        fun List<TemplateTask>.flatten(): List<TemplateTask> {
            return flatMap {
                listOf(it.copy(children = listOf())) + it.children.flatten()
            }
        }
    }
}
