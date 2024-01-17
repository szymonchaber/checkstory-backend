package tech.checkstory.template.data

import tech.checkstory.core.domain.auth.AuthenticatedScope
import tech.checkstory.data.EventRepository
import tech.checkstory.template.domain.TemplateRepository
import tech.checkstory.template.domain.aggregate.Template
import tech.checkstory.template.domain.event.TemplateEvent
import java.util.*

class EventSourcedTemplateRepository(
    private val templateEventRepository: EventRepository<TemplateEvent>
) : TemplateRepository {

    context(AuthenticatedScope)
    override suspend fun readAll(): List<Template> {
        return templateEventRepository.getAllGrouped()
            .asSequence()
            .mapNotNull { (id, events) ->
                TemplateAggregate.fromHistory(id, events).toTemplateOrNull()
            }
            .filter {
                it.userId == principal.userId
            }
            .filterNot {
                it.isDeleted
            }
            .toList()
    }

    override suspend fun read(id: UUID): Template? {
        return TemplateAggregate.fromHistory(id, templateEventRepository.getEventsForAggregate(id)).toTemplateOrNull()
    }

    suspend fun getByIdOrNew(id: UUID): TemplateAggregate {
        return TemplateAggregate.fromHistory(id, templateEventRepository.getEventsForAggregate(id))
    }
}
