package tech.checkstory.checklist.api

import org.kodein.di.DI
import org.kodein.di.instance
import tech.checkstory.checklist.domain.ChecklistRepository
import tech.checkstory.checklist.domain.aggregate.ChecklistAggregate
import tech.checkstory.checklist.domain.aggregate.ChecklistState
import tech.checkstory.checklist.domain.command.ChecklistCommand
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
            aggregate.applyCommands(commands, principal)
            val existingState = aggregate.checklistState as ChecklistState.Existing
            checklistRepository.upsert(existingState)
        }
}
