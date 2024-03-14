package tech.checkstory.template.data

import tech.checkstory.template.domain.aggregate.Template
import tech.checkstory.template.domain.event.TemplateEvent

class TemplateProjection(
    private val templateRepository: TemplateRepository
) {

    suspend fun handleEvents(events: List<TemplateEvent>) {
        events.forEach { handleEvent(it) }
    }

    private suspend fun handleEvent(event: TemplateEvent) {
        when (event) {
            is TemplateEvent.CreateTemplateEvent -> {
                createTemplate(event)
            }

            is TemplateEvent.DeleteTemplateTaskEvent -> {
                deleteTemplateTask(event)
            }
        }
    }

    private suspend fun createTemplate(createTemplate: TemplateEvent.CreateTemplateEvent) {
        with(createTemplate) {
            templateRepository.create(
                Template(
                    id = aggregateId,
                    userId = userId,
                    name = existingTemplateData?.title.orEmpty(),
                    description = existingTemplateData?.description.orEmpty(),
                    tasks = existingTemplateData?.tasks?.map { it.toTemplateTask(aggregateId) }.orEmpty(),
                    reminders = existingTemplateData?.reminders?.map { it.toReminder() }.orEmpty(),
                    createdAt = timestamp
                )
            )
        }
    }

    private suspend fun deleteTemplateTask(event: TemplateEvent.DeleteTemplateTaskEvent) {
        templateRepository.deleteTask(event.taskId)
    }

}
