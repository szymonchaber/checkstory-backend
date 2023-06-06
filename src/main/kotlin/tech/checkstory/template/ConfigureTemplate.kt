package tech.checkstory.template

import io.ktor.server.application.*
import org.jetbrains.exposed.sql.Database
import org.kodein.di.DI
import org.kodein.di.instance
import tech.checkstory.template.api.configureTemplateService
import tech.checkstory.template.data.EventSourcedTemplateRepository
import tech.checkstory.template.data.RelationalTemplateRepository

context(DI)
fun Application.configureTemplate() {
    val database by instance<Database>()
    val templateService = RelationalTemplateRepository(database)
    val eventSourcedTemplateRepository by instance<EventSourcedTemplateRepository>()
    configureTemplateService(templateService, eventSourcedTemplateRepository)
}
