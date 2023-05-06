package tech.checkstory.template

import com.eventstore.dbclient.EventStoreDBClient
import io.ktor.server.application.*
import org.jetbrains.exposed.sql.Database
import tech.checkstory.template.api.configureTemplateService
import tech.checkstory.template.api.configureTemplateTaskService
import tech.checkstory.template.data.EventBasedTemplateRepository
import tech.checkstory.template.data.RelationalTemplateRepository
import tech.checkstory.template.data.TemplateEventRepository
import tech.checkstory.template.data.TemplateTaskRepository

fun Application.configureTemplate(
    database: Database,
    client: EventStoreDBClient
) {
    val templateService = RelationalTemplateRepository(database)
    val templateTaskRepository = TemplateTaskRepository(database)
    val templateEventRepository = TemplateEventRepository(client)
    val eventBasedTemplateRepository = EventBasedTemplateRepository(templateEventRepository)
    configureTemplateService(templateService, eventBasedTemplateRepository)
    configureTemplateTaskService(templateService, templateTaskRepository)
    configureTemplateCommandService(templateEventRepository, eventBasedTemplateRepository)
}