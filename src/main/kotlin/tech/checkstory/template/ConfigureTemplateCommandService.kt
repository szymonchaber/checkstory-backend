package tech.checkstory.template

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import tech.checkstory.auth.firebase.FIREBASE_AUTH
import tech.checkstory.auth.payment.requirePayment
import tech.checkstory.auth.user.AuthenticatedScope
import tech.checkstory.auth.withPrincipal
import tech.checkstory.notification.firebase.UserDataChangeNotifier
import tech.checkstory.template.data.EventSourcedTemplateRepository
import tech.checkstory.template.data.TemplateEventRepository
import tech.checkstory.template.domain.command.TemplateCommand

fun Application.configureTemplateCommandService(
    templateEventRepository: TemplateEventRepository,
    eventSourcedTemplateRepository: EventSourcedTemplateRepository,
    userDataChangeNotifier: UserDataChangeNotifier
) {
    routing {
        authenticate(FIREBASE_AUTH) {
            requirePayment()
            post<List<TemplateCommand>>("/commands") { commands ->
                withPrincipal {
                    handleCommands(commands, templateEventRepository, eventSourcedTemplateRepository)
                    this@configureTemplateCommandService.log.debug("Notifying the user of changes")
                    userDataChangeNotifier.notifyUserOfDataChange()
                    call.respond(HttpStatusCode.NoContent)
                }
            }
        }
    }
}

context(AuthenticatedScope)
suspend fun handleCommands(
    commands: List<TemplateCommand>,
    templateEventRepository: TemplateEventRepository,
    eventSourcedTemplateRepository: EventSourcedTemplateRepository
) {
    commands
        .sortedBy { it.timestamp }
        .groupBy(TemplateCommand::templateId)
        .forEach { (templateId, commands) ->
            val events = eventSourcedTemplateRepository
                .getByIdOrNew(templateId)
                .applyCommands(principal, commands)
            templateEventRepository.saveAll(events, templateId)
        }
}
