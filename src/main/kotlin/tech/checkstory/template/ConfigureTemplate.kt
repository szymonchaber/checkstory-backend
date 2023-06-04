package tech.checkstory.template

import io.ktor.server.application.*
import org.jetbrains.exposed.sql.Database
import org.kodein.di.DI
import org.kodein.di.instance
import tech.checkstory.data.EventRepository
import tech.checkstory.notification.data.FirebaseMessagingTokenRepository
import tech.checkstory.notification.firebase.UserDataChangeNotifier
import tech.checkstory.template.api.configureTemplateService
import tech.checkstory.template.api.configureTemplateTaskService
import tech.checkstory.template.data.EventSourcedTemplateRepository
import tech.checkstory.template.data.RelationalTemplateRepository
import tech.checkstory.template.data.TemplateTaskRepository
import tech.checkstory.template.domain.event.TemplateEvent

context(DI)
fun Application.configureTemplate() {
    val database by instance<Database>()
    val templateService = RelationalTemplateRepository(database)
    val templateTaskRepository = TemplateTaskRepository(database)
    val templateEventRepository by instance<EventRepository<TemplateEvent>>()
    val eventSourcedTemplateRepository by instance<EventSourcedTemplateRepository>()
    val messagingTokenRepository by instance<FirebaseMessagingTokenRepository>()
    val userDataChangeNotifier = UserDataChangeNotifier(messagingTokenRepository, log)
    configureTemplateService(templateService, eventSourcedTemplateRepository)
    configureTemplateTaskService(templateService, templateTaskRepository)
    configureTemplateCommandService(
        templateEventRepository,
        eventSourcedTemplateRepository,
        userDataChangeNotifier
    )
}
