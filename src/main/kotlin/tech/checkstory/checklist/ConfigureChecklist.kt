package tech.checkstory.checklist

import io.ktor.server.application.*
import org.kodein.di.DI
import org.kodein.di.instance
import tech.checkstory.checklist.api.configureChecklistService
import tech.checkstory.checklist.data.EventBasedChecklistRepository
import tech.checkstory.checklist.domain.event.ChecklistEvent
import tech.checkstory.data.EventRepository
import tech.checkstory.template.data.EventSourcedTemplateRepository

context(DI)
fun Application.configureChecklist() {
    val templateRepository by instance<EventSourcedTemplateRepository>()
    val checklistEventRepository by instance<EventRepository<ChecklistEvent>>()
    val checklistRepository = EventBasedChecklistRepository(checklistEventRepository, templateRepository)
    configureChecklistService(checklistRepository)
}
