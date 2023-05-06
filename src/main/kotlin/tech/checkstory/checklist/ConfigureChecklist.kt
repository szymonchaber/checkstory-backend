package tech.checkstory.checklist

import io.ktor.server.application.*
import tech.checkstory.checklist.api.configureChecklistCommandService
import tech.checkstory.checklist.api.configureChecklistService
import tech.checkstory.checklist.data.ChecklistEventRepository
import tech.checkstory.checklist.data.EventBasedChecklistRepository

fun Application.configureChecklist(checklistEventRepository: ChecklistEventRepository) {
    val checklistRepository = EventBasedChecklistRepository(checklistEventRepository)
    configureChecklistService(checklistRepository)
    configureChecklistCommandService(checklistRepository, checklistEventRepository)
}