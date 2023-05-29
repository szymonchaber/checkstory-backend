package tech.checkstory.checklist.data

import tech.checkstory.auth.user.AuthenticatedScope
import tech.checkstory.checklist.domain.ChecklistRepository
import tech.checkstory.checklist.domain.aggregate.Checklist
import tech.checkstory.checklist.domain.aggregate.ChecklistAggregate
import tech.checkstory.domain.command.DtoUUID
import tech.checkstory.template.data.EventSourcedTemplateRepository
import java.util.*

class EventBasedChecklistRepository(
    private val eventRepository: ChecklistEventRepository,
    private val templateRepository: EventSourcedTemplateRepository
) : ChecklistRepository {

    context(AuthenticatedScope)
    override suspend fun readAll(): List<Checklist> {
        return eventRepository.getAllGrouped()
            .mapNotNull { (id, events) ->
                ChecklistAggregate.fromHistory(id, events).toChecklistOrNull()
            }
            .filter {
                it.userId == principal.userId
            }
            .filterNot(Checklist::isDeleted)
            .filterNot {
                isOrphaned(it)
            }
    }

    private suspend fun isOrphaned(checklist: Checklist): Boolean {
        return templateRepository.read(checklist.templateId)?.isDeleted == true
    }

    context(AuthenticatedScope)
    override suspend fun read(id: UUID): Checklist? {
        return getById(id).toChecklistOrNull()
    }

    context(AuthenticatedScope)
    suspend fun getByIdOrNew(checklistId: DtoUUID): ChecklistAggregate {
        return getById(checklistId)
    }

    context(AuthenticatedScope)
    suspend fun getById(id: UUID): ChecklistAggregate {
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
