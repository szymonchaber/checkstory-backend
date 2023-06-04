package tech.checkstory.checklist

import io.ktor.server.application.*
import org.kodein.di.DI
import org.kodein.di.instance
import tech.checkstory.checklist.api.configureChecklistCommandService
import tech.checkstory.checklist.api.configureChecklistService
import tech.checkstory.checklist.data.ChecklistEventRepository
import tech.checkstory.checklist.data.EventBasedChecklistRepository
import tech.checkstory.notification.firebase.UserDataChangeNotifier
import tech.checkstory.template.data.EventSourcedTemplateRepository

context(DI)
fun Application.configureChecklist() {
    val templateRepository by instance<EventSourcedTemplateRepository>()
    val userDataChangeNotifier by instance<UserDataChangeNotifier>()
    val checklistEventRepository by instance<ChecklistEventRepository>()
    val checklistRepository = EventBasedChecklistRepository(checklistEventRepository, templateRepository)
    configureChecklistService(checklistRepository)
    configureChecklistCommandService(checklistRepository, checklistEventRepository, userDataChangeNotifier)
}
