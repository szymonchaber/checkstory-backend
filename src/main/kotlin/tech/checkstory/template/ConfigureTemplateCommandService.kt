package tech.checkstory.template

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import tech.checkstory.auth.firebase.FIREBASE_AUTH
import tech.checkstory.auth.user.User
import tech.checkstory.auth.withFirebasePrincipal
import tech.checkstory.template.data.TemplateEventRepository
import tech.checkstory.template.domain.command.TemplateCommand
import tech.checkstory.template.domain.command.toTemplateEvent

fun Application.configureTemplateCommandService(
    templateEventRepository: TemplateEventRepository
) {
    routing {
        authenticate(FIREBASE_AUTH) {
            post<List<TemplateCommand>>("/commands") { commands ->
                withFirebasePrincipal {
                    handleCommands(it, commands, templateEventRepository)
                    call.respond(HttpStatusCode.NoContent)
                }
            }
        }
    }
}

suspend fun handleCommands(
    user: User,
    commands: List<TemplateCommand>,
    templateEventRepository: TemplateEventRepository
) {
    commands
        .sortedBy { it.timestamp }
        .forEach {
            // TODO also check if the userId in the existing stream is the same as requester's
            require(it.userId == user.userId)
            templateEventRepository.save(it.toTemplateEvent())
        }
}
