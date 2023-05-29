package tech.checkstory.checklist

import io.ktor.server.application.*
import tech.checkstory.checklist.api.configureChecklistCommandService
import tech.checkstory.checklist.api.configureChecklistService
import tech.checkstory.checklist.data.ChecklistEventRepository
import tech.checkstory.checklist.data.EventBasedChecklistRepository
import tech.checkstory.notification.firebase.UserDataChangeNotifier
import tech.checkstory.template.data.EventSourcedTemplateRepository

fun Application.configureChecklist(
    checklistEventRepository: ChecklistEventRepository,
    templateRepository: EventSourcedTemplateRepository,
    userDataChangeNotifier: UserDataChangeNotifier
) {
    val checklistRepository = EventBasedChecklistRepository(checklistEventRepository, templateRepository)
    configureChecklistService(checklistRepository)
    configureChecklistCommandService(checklistRepository, checklistEventRepository, userDataChangeNotifier)
}