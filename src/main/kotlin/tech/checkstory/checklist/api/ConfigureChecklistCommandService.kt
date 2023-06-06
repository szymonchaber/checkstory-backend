package tech.checkstory.checklist.api

import tech.checkstory.auth.user.AuthenticatedScope
import tech.checkstory.checklist.data.EventBasedChecklistRepository
import tech.checkstory.checklist.domain.command.ChecklistCommand
import tech.checkstory.checklist.domain.event.ChecklistEvent
import tech.checkstory.data.EventRepository

context(AuthenticatedScope)
suspend fun handleChecklistCommands(
    commands: List<ChecklistCommand>,
    repository: EventBasedChecklistRepository,
    eventRepository: EventRepository<ChecklistEvent>
) {
    commands.sortedBy { it.timestamp }
        .groupBy { it.checklistId }
        .forEach { (checklistId, commands) ->
            val events = repository.getByIdOrNew(checklistId)
                .applyCommands(commands, principal)
            eventRepository.saveAll(events, checklistId)
        }
}
