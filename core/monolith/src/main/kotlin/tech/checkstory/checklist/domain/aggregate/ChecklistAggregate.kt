package tech.checkstory.checklist.domain.aggregate

import kotlinx.datetime.Instant
import tech.checkstory.checklist.domain.command.ChecklistCommand
import tech.checkstory.checklist.domain.event.ChecklistEvent
import tech.checkstory.core.domain.DtoUUID
import tech.checkstory.core.domain.auth.User
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
                handleCreateChecklistCommand(checklistCommand, userId)
            }

            is ChecklistCommand.EditChecklistNotesCommand -> {
                ChecklistEvent.EditChecklistNotesEvent(
                    newNotes = checklistCommand.newNotes,
                    aggregateId = checklistCommand.checklistId,
                    eventId = checklistCommand.commandId,
                    timestamp = checklistCommand.timestamp,
                )
            }

            is ChecklistCommand.ChangeTaskCheckedCommand -> {
                ChecklistEvent.ChangeTaskCheckedEvent(
                    taskId = checklistCommand.taskId,
                    isChecked = checklistCommand.isChecked,
                    aggregateId = checklistCommand.checklistId,
                    eventId = checklistCommand.commandId,
                    timestamp = checklistCommand.timestamp
                )
            }

            is ChecklistCommand.DeleteChecklistCommand -> {
                ChecklistEvent.DeleteChecklistEvent(
                    aggregateId = checklistCommand.checklistId,
                    eventId = checklistCommand.commandId,
                    timestamp = checklistCommand.timestamp
                )
            }
        }
    }

    private fun handleCreateChecklistCommand(
        checklistCommand: ChecklistCommand.CreateChecklistCommand,
        userId: String
    ): ChecklistEvent.CreateChecklistEvent {
        fun Task.allTasksInHierarchyHaveChecklistId(): Boolean {
            return checklistId == checklistCommand.checklistId && children.all {
                it.allTasksInHierarchyHaveChecklistId()
            }
        }
        require(checklistCommand.tasks.all(Task::allTasksInHierarchyHaveChecklistId))
        return ChecklistEvent.CreateChecklistEvent(
            templateId = checklistCommand.templateId,
            title = checklistCommand.title,
            description = checklistCommand.description,
            tasks = checklistCommand.tasks,
            eventId = checklistCommand.commandId,
            timestamp = checklistCommand.timestamp,
            notes = checklistCommand.notes,
            userId = userId,
            aggregateId = id
        )
    }

    fun toChecklistOrNull(): Checklist? {
        return when (val state = checklistState) {
            is ChecklistState.Existing -> state.toChecklist()
            ChecklistState.NonExistent -> null
        }
    }

    companion object {

        fun fromChecklist(checklist: Checklist): ChecklistAggregate {
            return ChecklistAggregate(checklist.id).apply {
                checklistState = ChecklistState.Existing(
                    id = checklist.id,
                    userId = checklist.userId,
                    templateId = checklist.templateId,
                    title = checklist.title,
                    description = checklist.description,
                    notes = checklist.notes,
                    tasks = checklist.tasks,
                    createdAt = checklist.createdAt,
                    isDeleted = checklist.isDeleted
                )
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
        val createdAt: Instant,
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
                createdAt = createdAt,
                isDeleted = isDeleted,
            )
        }
    }
}
