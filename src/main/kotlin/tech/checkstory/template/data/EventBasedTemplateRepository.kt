package tech.checkstory.template.data

import tech.checkstory.auth.user.User
import tech.checkstory.domain.command.DtoUUID
import tech.checkstory.template.domain.TemplateRepository
import tech.checkstory.template.domain.aggregate.Reminder
import tech.checkstory.template.domain.aggregate.Template
import tech.checkstory.template.domain.aggregate.TemplateTask
import tech.checkstory.template.domain.command.TemplateCommand
import tech.checkstory.template.domain.event.TemplateEvent
import tech.checkstory.template.domain.event.TemplateEvent.CreateTemplateEvent
import tech.checkstory.template.domain.event.withExtractedTask
import tech.checkstory.template.domain.event.withMovedChildRecursive
import java.util.*

class EventBasedTemplateRepository(
    private val templateEventRepository: TemplateEventRepository
) : TemplateRepository {

    override suspend fun readAll(user: User): List<Template> {
        return templateEventRepository.getAll()
            .groupBy {
                it.templateId
            }
            .asSequence()
            .mapNotNull { (id, events) ->
                TemplateAggregate.fromHistory(id, events).toTemplateOrNull()
            }
            .filter {
                it.userId == user.userId
            }
            .filterNot {
                it.isDeleted
            }
            .toList()
    }

    override suspend fun read(user: User, id: UUID): Template? {
        return TemplateAggregate.fromHistory(id, templateEventRepository.getEventsForAggregate(id)).toTemplateOrNull()
    }

    suspend fun getByIdOrNew(id: UUID): TemplateAggregate {
        return TemplateAggregate.fromHistory(id, templateEventRepository.getEventsForAggregate(id))
    }
}

class TemplateAggregate(val id: UUID) {

    private var templateState: TemplateState = TemplateState.NonExistent

    fun applyEvent(templateEvent: TemplateEvent): TemplateAggregate {
        applyEventTo(templateState, templateEvent)
        return this
    }

    private fun applyEventTo(state: TemplateState, templateEvent: TemplateEvent) {
        when (templateEvent) {
            is CreateTemplateEvent -> {
                require(state is TemplateState.NonExistent) {
                    "Checklist should not exist when applying CreateTemplate event"
                }
                templateState = TemplateState.Existing(
                    userId = templateEvent.userId,
                    id = templateEvent.templateId,
                    name = "",
                    description = "",
                )
            }

            is TemplateEvent.AddOrUpdateReminderEvent -> {
                require(state is TemplateState.Existing) {
                    "Checklist should exist when applying AddOrUpdateReminder event"
                }
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
                require(state is TemplateState.Existing) {
                    "Checklist should exist when applying AddTemplateTask event"
                }
                require(state.userId == templateEvent.userId)
                val newItem =
                    TemplateTask(templateEvent.taskId, templateEvent.templateId, "", state.tasks.size.toLong())
                val items = if (templateEvent.parentTaskId == null) {
                    state.tasks.plus(newItem)
                } else {
                    state.tasks.map { it.withMovedChildRecursive(templateEvent.parentTaskId, newItem) }
                }
                templateState = state.copy(tasks = items)
            }

            is TemplateEvent.DeleteReminderEvent -> {
                require(state is TemplateState.Existing) {
                    "Checklist should exist when applying DeleteReminder event"
                }
                templateState = state.copy(reminders = state.reminders.filterNot { it.id == templateEvent.reminderId })
            }

            is TemplateEvent.DeleteTemplateEvent -> {
                require(state is TemplateState.Existing) {
                    "Checklist should exist when applying DeleteTemplate event"
                }
                templateState = state.copy(isDeleted = true)
            }

            is TemplateEvent.DeleteTemplateTaskEvent -> {
                require(state is TemplateState.Existing) {
                    "Checklist should exist when applying DeleteTemplateTask event"
                }
                require(state.userId == templateEvent.userId)
                val (filteredTasks, _) = state.tasks.withExtractedTask(templateEvent.taskId)
                templateState = state.copy(tasks = filteredTasks)
            }

            is TemplateEvent.EditTemplateDescriptionEvent -> {
                require(state is TemplateState.Existing) {
                    "Checklist should exist when applying EditTemplateDescription event"
                }
                require(state.userId == templateEvent.userId)
                templateState = state.copy(description = templateEvent.newDescription)
            }

            is TemplateEvent.EditTemplateTitleEvent -> {
                require(state is TemplateState.Existing) {
                    "Checklist should exist when applying EditTemplateTitle event"
                }
                require(state.userId == templateEvent.userId)
                templateState = state.copy(name = templateEvent.newTitle)
            }

            is TemplateEvent.MoveTemplateTaskEvent -> {
                require(state is TemplateState.Existing) {
                    "Checklist should exist when applying MoveTemplateTask event"
                }
                val (filteredTasks, movedItem) = state.tasks.withExtractedTask(templateEvent.taskId)
                val items = if (templateEvent.newParentTaskId == null) {
                    filteredTasks.plus(movedItem)
                } else {
                    filteredTasks.map { it.withMovedChildRecursive(templateEvent.newParentTaskId, movedItem) }
                }
                templateState = state.copy(tasks = items)
            }

            is TemplateEvent.RenameTemplateTaskEvent -> {
                require(state is TemplateState.Existing) {
                    "Checklist should exist when applying RenameTemplateTask event"
                }
                require(state.userId == templateEvent.userId)
                val newTasks = state.tasks.map {
                    it.withUpdatedTitleRecursive(templateEvent.taskId, templateEvent.newTitle)
                }
                templateState = state.copy(tasks = newTasks)
            }

            is TemplateEvent.UpdateTasksPositionsEvent -> {
                require(state is TemplateState.Existing) {
                    "Checklist should exist when applying UpdateTasksPositions event"
                }
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

    fun applyCommands(commands: List<TemplateCommand>): List<TemplateEvent> {
        return commands.map {
            val state = templateState
            require(state is TemplateState.NonExistent || it.userId == (state as TemplateState.Existing).userId) {
                "command user id must equal firebase user id"
            }
            it.toTemplateEvent()
        }
    }

    private fun TemplateCommand.toTemplateEvent(): TemplateEvent {
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

    companion object {

        fun fromHistory(id: DtoUUID, events: List<TemplateEvent>): TemplateAggregate {
            return events.fold(TemplateAggregate(id)) { aggregate, event ->
                aggregate.applyEvent(event)
            }
        }
    }
}

sealed interface TemplateState {

    data object NonExistent : TemplateState

    data class Existing(
        val id: DtoUUID,
        val userId: String,
        val name: String,
        val description: String,
        val tasks: List<TemplateTask> = listOf(),
        val reminders: List<Reminder> = listOf(),
        val isDeleted: Boolean = false
    ) : TemplateState {

        fun toTemplate(): Template {
            return Template(
                id = id,
                userId = userId,
                name = name,
                description = description,
                tasks = tasks,
                reminders = reminders,
                isDeleted = isDeleted,
            )
        }
    }
}