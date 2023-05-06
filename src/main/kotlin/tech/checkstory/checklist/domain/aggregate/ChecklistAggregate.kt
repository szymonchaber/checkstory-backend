package tech.checkstory.checklist.domain.aggregate

import tech.checkstory.auth.user.User
import tech.checkstory.checklist.domain.command.ChecklistCommand
import tech.checkstory.checklist.domain.event.ChecklistEvent
import tech.checkstory.domain.command.DtoUUID
import java.util.*

class ChecklistAggregate(val id: UUID) {

    private var checklistState: ChecklistState = ChecklistState.NonExistent

    fun isOwnedBy(user: User): Boolean {
        return when (val state = checklistState) {
            is ChecklistState.Existing -> state.userId == user.userId
            ChecklistState.NonExistent -> true
        }
    }

    fun applyCommands(commands: List<ChecklistCommand>, user: User): List<ChecklistEvent> {
        require(checklistState is ChecklistState.NonExistent || user.userId == (checklistState as ChecklistState.Existing).userId) {
            "commands' user id must equal firebase user id"
        }
        return commands.map {
            toChecklistEvent(it, user.userId)
        }
    }

    private fun toChecklistEvent(checklistCommand: ChecklistCommand, userId: String): ChecklistEvent {
        return when (checklistCommand) {
            is ChecklistCommand.CreateChecklistCommand -> {
                ChecklistEvent.CreateChecklistEvent(
                    checklistId = checklistCommand.checklistId,
                    templateId = checklistCommand.templateId,
                    title = checklistCommand.title,
                    description = checklistCommand.description,
                    tasks = checklistCommand.tasks,
                    eventId = checklistCommand.commandId,
                    timestamp = checklistCommand.timestamp,
                    userId = userId
                )
            }

            is ChecklistCommand.EditChecklistNotesCommand -> {
                ChecklistEvent.EditChecklistNotesEvent(
                    checklistCommand.checklistId,
                    checklistCommand.newNotes,
                    checklistCommand.commandId,
                    checklistCommand.timestamp
                )
            }

            is ChecklistCommand.ChangeTaskCheckedCommand -> {
                ChecklistEvent.ChangeTaskCheckedEvent(
                    checklistCommand.checklistId,
                    checklistCommand.taskId,
                    checklistCommand.isChecked,
                    checklistCommand.commandId,
                    checklistCommand.timestamp
                )
            }

            is ChecklistCommand.DeleteChecklistCommand -> {
                ChecklistEvent.DeleteChecklistEvent(
                    checklistCommand.checklistId,
                    checklistCommand.commandId,
                    checklistCommand.timestamp
                )
            }
        }
    }

    fun applyEvent(event: ChecklistEvent): ChecklistAggregate {
        val state = checklistState
        when (event) {
            is ChecklistEvent.CreateChecklistEvent -> {
                require(checklistState is ChecklistState.NonExistent) {
                    "Checklist should not exist when applying CreateChecklist event"
                }
                checklistState = ChecklistState.Existing(
                    id = event.checklistId,
                    templateId = event.templateId,
                    title = event.title,
                    description = event.description,
                    tasks = event.tasks,
                    userId = event.userId,
                    notes = ""
                )
            }

            is ChecklistEvent.ChangeTaskCheckedEvent -> {
                require(state is ChecklistState.Existing) {
                    "Checklist should exist when applying ChangeTaskCheckedEvent event"
                }
                val newTasks = state.tasks.map {
                    it.withUpdatedIsCheckedRecursive(event.taskId, event.isChecked)
                }
                checklistState = state.copy(tasks = newTasks)
            }

            is ChecklistEvent.DeleteChecklistEvent -> {
                require(state is ChecklistState.Existing) {
                    "Checklist should exist when applying DeleteChecklistEvent event"
                }
                checklistState = state.copy(isDeleted = true)
            }

            is ChecklistEvent.EditChecklistNotesEvent -> {
                require(state is ChecklistState.Existing) {
                    "Checklist should exist when applying EditChecklistNotesEvent event"
                }
                checklistState = state.copy(notes = event.newNotes)
            }
        }
        return this
    }

    fun toChecklistOrNull(): Checklist? {
        return when (val state = checklistState) {
            is ChecklistState.Existing -> state.toChecklist()
            ChecklistState.NonExistent -> null
        }
    }

    companion object {

        fun fromHistory(id: DtoUUID, events: List<ChecklistEvent>): ChecklistAggregate {
            return events.fold(ChecklistAggregate(id)) { aggregate, event ->
                aggregate.applyEvent(event)
            }
        }
    }
}

sealed interface ChecklistState {

    data object NonExistent : ChecklistState

    data class Existing(
        val id: DtoUUID,
        val userId: String,
        val templateId: DtoUUID,
        val title: String,
        val description: String,
        val notes: String,
        val tasks: List<Task> = listOf(),
        val isDeleted: Boolean = false
    ) : ChecklistState {

        fun toChecklist(): Checklist {
            return Checklist(
                id = id,
                userId = userId,
                templateId = templateId,
                title = title,
                description = description,
                notes = notes,
                tasks = tasks,
                isDeleted = isDeleted,
            )
        }
    }
}
