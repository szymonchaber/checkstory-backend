package tech.checkstory.checklist.data

import tech.checkstory.auth.user.User
import tech.checkstory.checklist.domain.ChecklistRepository
import tech.checkstory.checklist.domain.aggregate.Checklist
import tech.checkstory.checklist.domain.aggregate.ChecklistAggregate
import tech.checkstory.checklist.domain.event.ChecklistEvent
import tech.checkstory.domain.command.DtoUUID
import java.util.*

class EventBasedChecklistRepository(
    private val eventRepository: ChecklistEventRepository
) : ChecklistRepository {

    override suspend fun readAll(user: User): List<Checklist> {
        return eventRepository.getAll()
            .groupBy(ChecklistEvent::checklistId)
            .asSequence()
            .mapNotNull { (id, events) ->
                ChecklistAggregate.fromHistory(id, events).toChecklistOrNull()
            }
            .filter {
                it.userId == user.userId
            }
            .filterNot(Checklist::isDeleted)
            .toList()
    }

    override suspend fun read(user: User, id: UUID): Checklist? {
        return getById(user, id).toChecklistOrNull()
    }

    suspend fun getByIdOrNew(user: User, checklistId: DtoUUID): ChecklistAggregate {
        return getById(user, checklistId)
    }

    suspend fun getById(user: User, id: UUID): ChecklistAggregate {
        return eventRepository.getEventsForAggregate(id)
            .fold(new(id), ChecklistAggregate::applyEvent)
            .also {
                if (!it.isOwnedBy(user)) {
                    error("unauthorized access - 403")
                }
            }
    }

    private fun new(id: UUID): ChecklistAggregate {
        return ChecklistAggregate(id)
    }
}
