package tech.checkstory.checklist.api

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import tech.checkstory.auth.firebase.FIREBASE_AUTH
import tech.checkstory.auth.payment.requirePayment
import tech.checkstory.auth.user.AuthenticatedScope
import tech.checkstory.auth.withPrincipal
import tech.checkstory.checklist.data.ChecklistEventRepository
import tech.checkstory.checklist.data.EventBasedChecklistRepository
import tech.checkstory.checklist.domain.command.ChecklistCommand
import tech.checkstory.notification.firebase.UserDataChangeNotifier

fun Application.configureChecklistCommandService(
    repository: EventBasedChecklistRepository,
    eventRepository: ChecklistEventRepository,
    userDataChangeNotifier: UserDataChangeNotifier
) {
    routing {
        authenticate(FIREBASE_AUTH) {
            requirePayment()
            post<List<ChecklistCommand>>("/checklist-commands") { commands ->
                withPrincipal {
                    handleChecklistCommands(commands, repository, eventRepository)
                    userDataChangeNotifier.notifyUser()
                    call.respond(HttpStatusCode.NoContent)
                }
            }
        }
    }
}

context(AuthenticatedScope)
suspend fun handleChecklistCommands(
    commands: List<ChecklistCommand>,
    repository: EventBasedChecklistRepository,
    eventRepository: ChecklistEventRepository
) {
    commands.sortedBy { it.timestamp }
        .groupBy { it.checklistId }
        .forEach { (checklistId, commands) ->
            val events = repository.getByIdOrNew(checklistId)
                .applyCommands(commands, principal)
            eventRepository.saveAll(events, checklistId)
        }
}
