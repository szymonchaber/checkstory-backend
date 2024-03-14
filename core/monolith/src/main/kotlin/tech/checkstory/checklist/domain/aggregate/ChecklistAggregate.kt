package tech.checkstory.checklist.domain.aggregate

import kotlinx.datetime.Instant
import tech.checkstory.checklist.domain.command.ChecklistCommand
import tech.checkstory.core.domain.DtoUUID
import tech.checkstory.core.domain.auth.User
import java.util.*

class ChecklistAggregate(val id: UUID) {

    var checklistState: ChecklistState = ChecklistState.NonExistent

    fun isOwnedBy(user: User): Boolean {
        return when (val state = checklistState) {
            is ChecklistState.Existing -> state.userId == user.userId
            ChecklistState.NonExistent -> true
        }
    }

    fun applyCommands(commands: List<ChecklistCommand>, user: User) {
        require(checklistState is ChecklistState.NonExistent || user.userId == (checklistState as ChecklistState.Existing).userId) {
            "commands' user id must equal authenticated user id"
        }
        commands.forEach {
            applyCommand(it, user.userId)
        }
    }

    private fun applyCommand(checklistCommand: ChecklistCommand, userId: String) {
        when (checklistCommand) {
            is ChecklistCommand.CreateChecklistCommand -> {
                handleCreateChecklistCommand(checklistCommand, userId)
            }

            is ChecklistCommand.EditChecklistNotesCommand -> {
                checklistState = (checklistState as ChecklistState.Existing).copy(notes = checklistCommand.newNotes)
            }

            is ChecklistCommand.ChangeTaskCheckedCommand -> {
                checklistState = (checklistState as ChecklistState.Existing)
                    .copy(
                        tasks = (checklistState as ChecklistState.Existing).tasks.map {
                            if (it.id == checklistCommand.taskId) {
                                it.copy(isChecked = checklistCommand.isChecked)
                            } else {
                                it
                            }
                        }
                    )
            }

            is ChecklistCommand.DeleteChecklistCommand -> {
                checklistState = (checklistState as ChecklistState.Existing).copy(isDeleted = true)
            }
        }
    }

    private fun handleCreateChecklistCommand(
        checklistCommand: ChecklistCommand.CreateChecklistCommand,
        userId: String
    ) {
        fun Task.allTasksInHierarchyHaveChecklistId(): Boolean {
            return checklistId == checklistCommand.checklistId && children.all {
                it.allTasksInHierarchyHaveChecklistId()
            }
        }
        require(checklistCommand.tasks.all(Task::allTasksInHierarchyHaveChecklistId))
        checklistState = ChecklistState.Existing(
            id = checklistCommand.checklistId,
            userId = userId,
            templateId = checklistCommand.templateId,
            notes = checklistCommand.notes.orEmpty(),
            tasks = checklistCommand.tasks,
            createdAt = checklistCommand.timestamp,
            isDeleted = false
        )
    }

    companion object {

        fun fromChecklist(checklist: Checklist): ChecklistAggregate {
            return ChecklistAggregate(checklist.id).apply {
                checklistState = ChecklistState.Existing(
                    id = checklist.id,
                    userId = checklist.userId,
                    templateId = checklist.templateId,
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
        val notes: String,
        val tasks: List<Task> = listOf(),
        val createdAt: Instant,
        val isDeleted: Boolean = false
    ) : ChecklistState
}
