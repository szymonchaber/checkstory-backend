package tech.checkstory.template

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import tech.checkstory.auth.firebase.FIREBASE_AUTH
import tech.checkstory.auth.user.User
import tech.checkstory.auth.withFirebasePrincipal
import tech.checkstory.template.data.EventBasedTemplateRepository
import tech.checkstory.template.data.TemplateEventRepository
import tech.checkstory.template.domain.command.TemplateCommand

fun Application.configureTemplateCommandService(
    templateEventRepository: TemplateEventRepository,
    eventBasedTemplateRepository: EventBasedTemplateRepository
) {
    routing {
        authenticate(FIREBASE_AUTH) {
            post<List<TemplateCommand>>("/commands") { commands ->
                withFirebasePrincipal {
                    handleCommands(it, commands, templateEventRepository, eventBasedTemplateRepository)
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
    eventBasedTemplateRepository: EventBasedTemplateRepository
) {
    commands
        .sortedBy { it.timestamp }
        .onEach {
            require(it.userId == user.userId) {
                "command user id must equal firebase user id"
            }
        }
        .groupBy(TemplateCommand::templateId)
        .forEach { (templateId, commands) ->
            eventBasedTemplateRepository.getByIdOrNew(templateId)
                .applyCommands(commands)
                .forEach {
                    templateEventRepository.save(it)
                }
        }
}
