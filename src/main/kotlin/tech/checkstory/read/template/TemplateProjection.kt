package tech.checkstory.read.template

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filterIsInstance
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
                .filterIsInstance<TemplateEvent>()
                .onEach {
                    when (it) {
                        is TemplateEvent.CreateTemplateEvent -> {
                            createTemplate(it)
                        }

                        is TemplateEvent.AddOrUpdateReminderEvent -> {
                            println("Encountered AddOrUpdateReminderEvent")
                        }

                        is TemplateEvent.AddTemplateTaskEvent -> {
                            println("Encountered AddTemplateTaskEvent")
                        }

                        is TemplateEvent.DeleteReminderEvent -> {
                            println("Encountered DeleteReminderEvent")
                        }

                        is TemplateEvent.DeleteTemplateEvent -> {
                            println("Encountered DeleteTemplateEvent")
                        }

                        is TemplateEvent.DeleteTemplateTaskEvent -> {
                            println("Encountered DeleteTemplateTaskEvent")
                        }

                        is TemplateEvent.EditTemplateDescriptionEvent -> {
                            println("Encountered EditTemplateDescriptionEvent")
                        }

                        is TemplateEvent.EditTemplateTitleEvent -> {
                            println("Encountered EditTemplateTitleEvent")
                        }

                        is TemplateEvent.MoveTemplateTaskEvent -> {
                            println("Encountered MoveTemplateTaskEvent")
                        }

                        is TemplateEvent.RenameTemplateTaskEvent -> {
                            println("Encountered RenameTemplateTaskEvent")
                        }

                        is TemplateEvent.UpdateTasksPositionsEvent -> {
                            println("Encountered UpdateTasksPositionsEvent")
                        }
                    }
                }
                .collect()
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
}
