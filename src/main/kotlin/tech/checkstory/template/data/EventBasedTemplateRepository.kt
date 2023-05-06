package tech.checkstory.template.data

import tech.checkstory.auth.user.User
import tech.checkstory.template.domain.TemplateRepository
import tech.checkstory.template.domain.aggregate.Template
import tech.checkstory.template.domain.event.TemplateEvent
import tech.checkstory.template.domain.event.TemplateEvent.CreateTemplateEvent
import tech.checkstory.template.extensions.requireStartsWith
import java.util.*

class EventBasedTemplateRepository(
    private val templateEventRepository: TemplateEventRepository
) : TemplateRepository {

    override suspend fun readAll(user: User): List<Template> {
        return templateEventRepository.getAll()
            .groupBy {
                it.templateId
            }
            .asSequence()
            .map { (_, events) ->
                val (createTemplate, otherEvents) = events.requireStartsWith<TemplateEvent, CreateTemplateEvent>()
                TemplateAggregate(createTemplate, otherEvents).template
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
        return templateEventRepository.getEventsForAggregate(id)
            .takeUnless { it.isEmpty() }
            ?.fold(
                Template(
                    id = id,
                    userId = "",
                    name = "",
                    description = ""
                )
            ) { acc, checklistTemplateEvent ->
                checklistTemplateEvent.applyTo(acc)
            }
    }
}

class TemplateAggregate(
    createTemplateEvent: CreateTemplateEvent,
    otherEvents: List<TemplateEvent>
) {

    val template: Template

    init {
        val createdTemplate = Template(
            id = createTemplateEvent.templateId,
            userId = createTemplateEvent.userId,
            "",
            "",
        )
        template = otherEvents.fold(createdTemplate) { acc, event ->
            event.applyTo(acc)
        }
    }
}
