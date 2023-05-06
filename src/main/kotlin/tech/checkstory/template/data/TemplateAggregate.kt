package tech.checkstory.template.data

import tech.checkstory.auth.user.User
import tech.checkstory.domain.command.DtoUUID
import tech.checkstory.domain.event.CreationEvent
import tech.checkstory.domain.event.MutationEvent
import tech.checkstory.template.domain.aggregate.Template
import tech.checkstory.template.domain.aggregate.TemplateTask
import tech.checkstory.template.domain.command.TemplateCommand
import tech.checkstory.template.domain.event.TemplateEvent
import tech.checkstory.template.domain.event.withExtractedTask
import tech.checkstory.template.domain.event.withMovedChildRecursive
import java.util.*

class TemplateAggregate(val id: UUID) {

    private var templateState: TemplateState = TemplateState.NonExistent

    fun applyEvent(templateEvent: TemplateEvent): TemplateAggregate {
        applyEventTo(templateEvent)
        return this
    }

    private fun applyEventTo(templateEvent: TemplateEvent) {
        when (templateEvent) {
            is MutationEvent -> {
                val state = templateState
                require(state is TemplateState.Existing) {
                    "Checklist should exist when applying a mutation event"
                }
                applyMutationEvent(state, templateEvent)
            }

            is CreationEvent -> {
                val state = templateState
                require(state is TemplateState.NonExistent) {
                    "Checklist should not exist when applying a creation event"
                }
                when (templateEvent) {
                    is TemplateEvent.CreateTemplateEvent -> {
                        require(templateState is TemplateState.NonExistent) {
                            "Checklist should not exist when applying CreateTemplate event"
                        }
                        templateState = TemplateState.Existing(
                            userId = templateEvent.userId,
                            id = templateEvent.templateId,
                            name = "",
                            description = "",
                        )
                    }

                    else -> Unit
                }
            }

            else -> error("There is an event not marked a creation nor a mutation")
        }
    }

    private fun applyMutationEvent(state: TemplateState.Existing, templateEvent: TemplateEvent) {
        return when (templateEvent) {
            is TemplateEvent.CreateTemplateEvent -> Unit
            is TemplateEvent.AddOrUpdateReminderEvent -> {
                val reminder = templateEvent.reminder.toReminder()
                val newReminders = if (state.reminders.any { it.id == reminder.id }) {
                    state.reminders.map {
                        if (it.id == reminder.id) {
                            reminder
                        } else {
                            it
                        }
                    }
                } else {
                    state.reminders.plus(reminder)
                }
                templateState = state.copy(reminders = newReminders)
            }

            is TemplateEvent.AddTemplateTaskEvent -> {
                val newItem =
                    TemplateTask(
                        templateEvent.taskId, templateEvent.templateId, "",
                        state.tasks.size.toLong()
                    )
                val items = if (templateEvent.parentTaskId == null) {
                    state.tasks.plus(newItem)
                } else {
                    state.tasks.map { it.withMovedChildRecursive(templateEvent.parentTaskId, newItem) }
                }
                templateState = state.copy(tasks = items)
            }

            is TemplateEvent.DeleteReminderEvent -> {
                templateState =
                    state.copy(reminders = state.reminders.filterNot { it.id == templateEvent.reminderId })
            }

            is TemplateEvent.DeleteTemplateEvent -> {
                templateState = state.copy(isDeleted = true)
            }

            is TemplateEvent.DeleteTemplateTaskEvent -> {
                val (filteredTasks, _) = state.tasks.withExtractedTask(templateEvent.taskId)
                templateState = state.copy(tasks = filteredTasks)
            }

            is TemplateEvent.EditTemplateDescriptionEvent -> {
                templateState = state.copy(description = templateEvent.newDescription)
            }

            is TemplateEvent.EditTemplateTitleEvent -> {
                templateState = state.copy(name = templateEvent.newTitle)
            }

            is TemplateEvent.MoveTemplateTaskEvent -> {
                val (filteredTasks, movedItem) = state.tasks.withExtractedTask(templateEvent.taskId)
                val items = if (templateEvent.newParentTaskId == null) {
                    filteredTasks.plus(movedItem)
                } else {
                    filteredTasks.map { it.withMovedChildRecursive(templateEvent.newParentTaskId, movedItem) }
                }
                templateState = state.copy(tasks = items)
            }

            is TemplateEvent.RenameTemplateTaskEvent -> {
                val newTasks = state.tasks.map {
                    it.withUpdatedTitleRecursive(templateEvent.taskId, templateEvent.newTitle)
                }
                templateState = state.copy(tasks = newTasks)
            }

            is TemplateEvent.UpdateTasksPositionsEvent -> {
                templateState = state.copy(
                    tasks = state.tasks.map {
                        updatePosition(it, templateEvent.taskIdToLocalPosition)
                    }
                        .sortedBy(TemplateTask::sortPosition)
                )
            }
        }
    }

    private fun updatePosition(
        templateCheckbox: TemplateTask,
        taskIdToLocalPosition: Map<DtoUUID, Long>
    ): TemplateTask {
        val newPosition = taskIdToLocalPosition[templateCheckbox.id] ?: templateCheckbox.sortPosition
        return templateCheckbox
            .copy(
                sortPosition = newPosition,
                children = templateCheckbox.children.map { updatePosition(it, taskIdToLocalPosition) }
                    .sortedBy(TemplateTask::sortPosition)
            )
    }

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
                )
            }

            is TemplateCommand.EditTemplateDescriptionCommand -> {
                TemplateEvent.EditTemplateDescriptionEvent(
                    templateId,
                    newDescription,
                    commandId,
                    timestamp,
                )
            }

            is TemplateCommand.AddTemplateTaskCommand -> {
                TemplateEvent.AddTemplateTaskEvent(
                    templateId,
                    taskId,
                    parentTaskId,
                    commandId,
                    timestamp,
                )
            }

            is TemplateCommand.RenameTemplateTaskCommand -> {
                TemplateEvent.RenameTemplateTaskEvent(
                    templateId,
                    taskId,
                    newTitle,
                    commandId,
                    timestamp,
                )
            }

            is TemplateCommand.DeleteTemplateTaskCommand -> {
                TemplateEvent.DeleteTemplateTaskEvent(
                    templateId,
                    taskId,
                    commandId,
                    timestamp,
                )
            }

            is TemplateCommand.UpdateTasksPositionsCommandDto -> {
                TemplateEvent.UpdateTasksPositionsEvent(
                    templateId,
                    taskIdToLocalPosition,
                    commandId,
                    timestamp,
                )
            }

            is TemplateCommand.MoveTemplateTaskCommandDto -> {
                TemplateEvent.MoveTemplateTaskEvent(
                    templateId,
                    taskId,
                    newParentTaskId,
                    commandId,
                    timestamp,
                )
            }

            is TemplateCommand.AddOrUpdateTemplateReminderCommandDto -> {
                TemplateEvent.AddOrUpdateReminderEvent(
                    templateId,
                    reminder,
                    commandId,
                    timestamp,
                )
            }

            is TemplateCommand.DeleteTemplateReminderCommandDto -> {
                TemplateEvent.DeleteReminderEvent(
                    templateId,
                    reminderId,
                    commandId,
                    timestamp,
                )
            }

            is TemplateCommand.DeleteTemplateCommandDto -> {
                TemplateEvent.DeleteTemplateEvent(
                    templateId,
                    commandId,
                    timestamp,
                )
            }
        }
    }

    companion object {

        fun fromHistory(id: DtoUUID, events: List<TemplateEvent>): TemplateAggregate {
            return events.fold(TemplateAggregate(id)) { aggregate, event ->
                aggregate.applyEvent(event)
            }
        }
    }
}