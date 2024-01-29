package tech.checkstory.checklist

import io.ktor.server.application.*
import org.kodein.di.DI
import org.kodein.di.instance
import tech.checkstory.checklist.api.configureChecklistService
import tech.checkstory.checklist.domain.ChecklistRepository

context(DI)
fun Application.configureChecklist() {
    val checklistRepository by instance<ChecklistRepository>()
    configureChecklistService(checklistRepository)
}
