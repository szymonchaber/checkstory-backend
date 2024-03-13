package tech.checkstory.checklist.api

import org.kodein.di.DI
import org.kodein.di.instance
import tech.checkstory.checklist.domain.ChecklistRepository
import tech.checkstory.checklist.domain.aggregate.ChecklistAggregate
import tech.checkstory.checklist.domain.command.ChecklistCommand
import tech.checkstory.checklist.domain.event.ChecklistEvent
import tech.checkstory.core.domain.auth.AuthenticatedScope

context(AuthenticatedScope, DI)
suspend fun handleChecklistCommands(commands: List<ChecklistCommand>) {
    val checklistRepository by instance<ChecklistRepository>()
    commands.sortedBy { it.timestamp }
        .groupBy { it.checklistId }
        .forEach { (checklistId, commands) ->
            val aggregate = checklistRepository.read(checklistId)?.let {
                if (it.userId != principal.userId) {
                    error("unauthorized access - 403")
                }
                ChecklistAggregate.fromChecklist(it)
            } ?: ChecklistAggregate(checklistId)
            val acceptedEvents = aggregate
                .applyCommands(commands, principal)
            // TODO: we should persist aggregate state directly
            acceptedEvents.forEach {
                when (it) {
                    is ChecklistEvent.CreateChecklistEvent -> {
                        checklistRepository.create(it)
                    }

                    is ChecklistEvent.EditChecklistNotesEvent -> {
                        checklistRepository.editNotes(it.aggregateId, it.newNotes)
                    }

                    is ChecklistEvent.ChangeTaskCheckedEvent -> {
                        checklistRepository.changeTaskChecked(it.taskId, it.isChecked)
                    }

                    is ChecklistEvent.DeleteChecklistEvent -> {
                        checklistRepository.delete(it.aggregateId)
                    }
                }
            }
        }
}
