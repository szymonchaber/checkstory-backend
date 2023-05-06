package tech.checkstory.checklist.api

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import tech.checkstory.auth.firebase.FIREBASE_AUTH
import tech.checkstory.auth.user.User
import tech.checkstory.auth.withFirebasePrincipal
import tech.checkstory.checklist.data.ChecklistEventRepository
import tech.checkstory.checklist.data.EventBasedChecklistRepository
import tech.checkstory.checklist.domain.command.ChecklistCommand

fun Application.configureChecklistCommandService(
    repository: EventBasedChecklistRepository,
    eventRepository: ChecklistEventRepository
) {
    routing {
        authenticate(FIREBASE_AUTH) {
            post<List<ChecklistCommand>>("/checklist-commands") { commands ->
                withFirebasePrincipal {
                    handleChecklistCommands(it, commands, repository, eventRepository)
                    call.respond(HttpStatusCode.NoContent)
                }
            }
        }
    }
}

suspend fun handleChecklistCommands(
    user: User,
    commands: List<ChecklistCommand>,
    repository: EventBasedChecklistRepository,
    eventRepository: ChecklistEventRepository
) {
    commands.sortedBy { it.timestamp }
        .onEach {
            require(it.userId == user.userId) {
                "command user id must equal firebase user id"
            }
        }
        .groupBy { it.checklistId }
        .forEach { (checklistId, commands) ->
            repository.getByIdOrNew(user, checklistId)
                .applyCommands(commands)
                .forEach {
                    eventRepository.save(it)
                }
        }
}
