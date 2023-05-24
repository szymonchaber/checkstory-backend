package tech.checkstory.template

import io.ktor.server.application.*
import org.jetbrains.exposed.sql.Database
import tech.checkstory.template.api.configureTemplateService
import tech.checkstory.template.api.configureTemplateTaskService
import tech.checkstory.template.data.EventSourcedTemplateRepository
import tech.checkstory.template.data.RelationalTemplateRepository
import tech.checkstory.template.data.TemplateEventRepository
import tech.checkstory.template.data.TemplateTaskRepository

fun Application.configureTemplate(
    database: Database,
    templateEventRepository: TemplateEventRepository,
    eventSourcedTemplateRepository: EventSourcedTemplateRepository
) {
    val templateService = RelationalTemplateRepository(database)
    val templateTaskRepository = TemplateTaskRepository(database)
    configureTemplateService(templateService, eventSourcedTemplateRepository)
    configureTemplateTaskService(templateService, templateTaskRepository)
    configureTemplateCommandService(templateEventRepository, eventSourcedTemplateRepository)
}