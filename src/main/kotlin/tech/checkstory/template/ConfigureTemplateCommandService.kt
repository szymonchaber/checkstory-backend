package tech.checkstory.template

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import tech.checkstory.auth.firebase.FIREBASE_AUTH
import tech.checkstory.auth.user.User
import tech.checkstory.auth.withFirebasePrincipal
import tech.checkstory.template.data.EventSourcedTemplateRepository
import tech.checkstory.template.data.TemplateEventRepository
import tech.checkstory.template.domain.command.TemplateCommand

fun Application.configureTemplateCommandService(
    templateEventRepository: TemplateEventRepository,
    eventSourcedTemplateRepository: EventSourcedTemplateRepository
) {
    routing {
        authenticate(FIREBASE_AUTH) {
            post<List<TemplateCommand>>("/commands") { commands ->
                withFirebasePrincipal {
                    handleCommands(it, commands, templateEventRepository, eventSourcedTemplateRepository)
                    call.respond(HttpStatusCode.NoContent)
                }
            }
        }
    }
}

suspend fun handleCommands(
    user: User,
    commands: List<TemplateCommand>,
    templateEventRepository: TemplateEventRepository,
    eventSourcedTemplateRepository: EventSourcedTemplateRepository
) {
    commands
        .sortedBy { it.timestamp }
        .groupBy(TemplateCommand::templateId)
        .forEach { (templateId, commands) ->
            eventSourcedTemplateRepository.getByIdOrNew(templateId)
                .applyCommands(user, commands)
                .forEach {
                    templateEventRepository.save(it)
                }
        }
}
