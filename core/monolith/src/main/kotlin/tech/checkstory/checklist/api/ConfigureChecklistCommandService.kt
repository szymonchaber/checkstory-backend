package tech.checkstory.checklist.api

import tech.checkstory.checklist.data.EventBasedChecklistRepository
import tech.checkstory.checklist.domain.command.ChecklistCommand
import tech.checkstory.checklist.domain.event.ChecklistEvent
import tech.checkstory.core.domain.auth.AuthenticatedScope
import tech.checkstory.data.EventRepository
import tech.checkstory.read.checklist.ChecklistProjection

context(AuthenticatedScope)
suspend fun handleChecklistCommands(
    commands: List<ChecklistCommand>,
    repository: EventBasedChecklistRepository,
    eventRepository: EventRepository<ChecklistEvent>,
    checklistProjection: ChecklistProjection
) {
    commands.sortedBy { it.timestamp }
        .groupBy { it.checklistId }
        .forEach { (checklistId, commands) ->
            val events = repository.getByIdOrNew(checklistId)
                .applyCommands(commands, principal)
            val eventsWithoutEditNotesChangeEvent = events.filterNot { it is ChecklistEvent.EditChecklistNotesEvent }
            eventRepository.saveAll(eventsWithoutEditNotesChangeEvent, checklistId)
            events.filterIsInstance<ChecklistEvent.EditChecklistNotesEvent>().forEach {
                checklistProjection.editChecklistNotes(it)
            }
        }
}
