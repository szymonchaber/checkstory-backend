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

            is TemplateEvent.AddTemplateTaskEvent -> {
                addTemplateTask(event)
            }

            is TemplateEvent.RenameTemplateTaskEvent -> {
                renameTemplateTask(event)
            }

            is TemplateEvent.MoveTemplateTaskEvent -> {
                moveTemplateTask(event)
            }

            is TemplateEvent.UpdateTasksPositionsEvent -> {
                updatePositions(event)
            }

            is TemplateEvent.DeleteTemplateTaskEvent -> {
                deleteTemplateTask(event)
            }

            is TemplateEvent.AddOrUpdateReminderEvent -> {
                addOrUpdateReminder(event)
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

    private suspend fun renameTemplateTask(event: TemplateEvent.RenameTemplateTaskEvent) {
        templateRepository.renameTask(event.taskId, event.newTitle)
    }

    private suspend fun addTemplateTask(event: TemplateEvent.AddTemplateTaskEvent) {
        with(event) {
            templateRepository.addTask(aggregateId, taskId, parentTaskId)
        }
    }

    private suspend fun moveTemplateTask(event: TemplateEvent.MoveTemplateTaskEvent) {
        templateRepository.moveTask(event.taskId, event.newParentTaskId)
    }

    private suspend fun updatePositions(event: TemplateEvent.UpdateTasksPositionsEvent) {
        templateRepository.updateLocalPositions(event.taskIdToLocalPosition)
    }

    private suspend fun deleteTemplateTask(event: TemplateEvent.DeleteTemplateTaskEvent) {
        templateRepository.deleteTask(event.taskId)
    }

    private suspend fun addOrUpdateReminder(event: TemplateEvent.AddOrUpdateReminderEvent) {
        templateRepository.addReminder(event.reminder.toReminder())
    }
}
