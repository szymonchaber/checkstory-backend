package tech.checkstory.template.module

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.kodein.di.DI
import org.kodein.di.instance
import tech.checkstory.auth.firebase.FIREBASE_AUTH
import tech.checkstory.auth.payment.requirePayment
import tech.checkstory.auth.withPrincipal
import tech.checkstory.checklist.api.handleChecklistCommands
import tech.checkstory.command.CommandsPayload
import tech.checkstory.core.domain.auth.AuthenticatedScope
import tech.checkstory.notification.firebase.UserDataChangeNotifier
import tech.checkstory.template.data.TemplateAggregate
import tech.checkstory.template.data.TemplateRepository
import tech.checkstory.template.data.TemplateState
import tech.checkstory.template.domain.command.TemplateCommand

context(DI)
fun Application.configureSynchronization() {
    val userDataChangeNotifier by instance<UserDataChangeNotifier>()
    routing {
        authenticate(FIREBASE_AUTH) {
            requirePayment()
            post<CommandsPayload>("/sync/commands") { (templateCommands, checklistCommands) ->
                withPrincipal {
                    withContext(Dispatchers.Default) {
                        handleTemplateCommands(templateCommands)
                        handleChecklistCommands(checklistCommands)
                    }
                    userDataChangeNotifier.notifyUserOfDataChange(principal.userId)
                    call.respond<HttpStatusCode>(HttpStatusCode.NoContent)
                }
            }
        }
    }
}

context(AuthenticatedScope, DI)
suspend fun handleTemplateCommands(
    commands: List<TemplateCommand>,
) {
    val templateRepository by instance<TemplateRepository>()

    commands
        .sortedBy { it.timestamp }
        .groupBy(TemplateCommand::templateId)
        .forEach { (templateId, commands) ->
            val aggregate = templateRepository.read(templateId)?.let {
                if (it.userId != principal.userId) {
                    error("unauthorized access - 403")
                }
                TemplateAggregate.fromTemplate(it)
            } ?: TemplateAggregate(templateId)
            val updatedState = aggregate.applyCommands(principal, commands)
            templateRepository.upsert(updatedState as TemplateState.Existing)
        }
}
