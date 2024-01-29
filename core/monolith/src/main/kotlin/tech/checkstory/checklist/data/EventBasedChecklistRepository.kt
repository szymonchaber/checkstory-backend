package tech.checkstory.checklist.data

import tech.checkstory.checklist.domain.aggregate.ChecklistAggregate
import tech.checkstory.checklist.domain.event.ChecklistEvent
import tech.checkstory.core.domain.DtoUUID
import tech.checkstory.core.domain.auth.AuthenticatedScope
import tech.checkstory.data.EventRepository
import java.util.*

class EventBasedChecklistRepository(
    private val eventRepository: EventRepository<ChecklistEvent>
) {

    context(AuthenticatedScope)
    suspend fun getByIdOrNew(checklistId: DtoUUID): ChecklistAggregate {
        return getById(checklistId)
    }

    context(AuthenticatedScope)
    private suspend fun getById(id: UUID): ChecklistAggregate {
        return eventRepository.getEventsForAggregate(id)
            .fold(new(id), ChecklistAggregate::applyEvent)
            .also {
                if (!it.isOwnedBy(principal)) {
                    error("unauthorized access - 403")
                }
            }
    }

    private fun new(id: UUID): ChecklistAggregate {
        return ChecklistAggregate(id)
    }
}
