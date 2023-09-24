package tech.checkstory.read.template

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import tech.checkstory.eventbus.EventBus
import tech.checkstory.template.data.RelationalTemplateRepository
import tech.checkstory.template.domain.Template
import tech.checkstory.template.domain.event.TemplateEvent

class TemplateProjection(
    private val eventBus: EventBus,
    private val templateRepository: RelationalTemplateRepository
) {

    suspend fun run() {
        withContext(Dispatchers.Default) {
            eventBus
                .events
                .map { it.event }
                .filterIsInstance<TemplateEvent>()
                .onEach(::handleEvent)
                .collect()
        }
    }

    private suspend fun handleEvent(event: TemplateEvent) {
        when (event) {
            is TemplateEvent.CreateTemplateEvent -> {
                createTemplate(event)
            }

            is TemplateEvent.EditTemplateTitleEvent -> {
                editTitle(event)
            }

            is TemplateEvent.EditTemplateDescriptionEvent -> {
                editDescription(event)
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

            is TemplateEvent.DeleteReminderEvent -> {
                deleteReminder(event)
            }

            is TemplateEvent.DeleteTemplateEvent -> {
                deleteTemplate(event)
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

    private suspend fun editTitle(event: TemplateEvent.EditTemplateTitleEvent) {
        templateRepository.updateTitle(event.aggregateId, event.newTitle)
    }

    private suspend fun editDescription(event: TemplateEvent.EditTemplateDescriptionEvent) {
        templateRepository.updateDescription(event.aggregateId, event.newDescription)
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

    private suspend fun deleteReminder(event: TemplateEvent.DeleteReminderEvent) {
        templateRepository.deleteReminder(event.reminderId)
    }

    private suspend fun deleteTemplate(event: TemplateEvent.DeleteTemplateEvent) {
        templateRepository.markAsDeleted(event.aggregateId)
    }
}
