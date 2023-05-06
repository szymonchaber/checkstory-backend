package tech.checkstory.template.data

import tech.checkstory.auth.user.User
import tech.checkstory.template.domain.TemplateRepository
import tech.checkstory.template.domain.aggregate.Template
import java.util.*

class EventSourcedTemplateRepository(
    private val templateEventRepository: TemplateEventRepository
) : TemplateRepository {

    override suspend fun readAll(user: User): List<Template> {
        return templateEventRepository.getAll()
            .groupBy {
                it.templateId
            }
            .asSequence()
            .mapNotNull { (id, events) ->
                TemplateAggregate.fromHistory(id, events).toTemplateOrNull()
            }
            .filter {
                it.userId == user.userId
            }
            .filterNot {
                it.isDeleted
            }
            .toList()
    }

    override suspend fun read(user: User, id: UUID): Template? {
        return TemplateAggregate.fromHistory(id, templateEventRepository.getEventsForAggregate(id)).toTemplateOrNull()
    }

    suspend fun getByIdOrNew(id: UUID): TemplateAggregate {
        return TemplateAggregate.fromHistory(id, templateEventRepository.getEventsForAggregate(id))
    }
}
