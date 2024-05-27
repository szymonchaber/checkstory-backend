package tech.checkstory

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.kodein.di.DI
import org.kodein.di.instance
import tech.checkstory.auth.firebase.authenticateFirebase
import tech.checkstory.auth.payment.requirePayment
import tech.checkstory.auth.withPrincipal
import tech.checkstory.checklist.module.handleChecklistCommands
import tech.checkstory.command.CommandsPayload
import tech.checkstory.notification.firebase.UserDataChangeNotifier
import tech.checkstory.template.module.handleTemplateCommands

context(DI)
fun Application.configureSynchronization() {
    val userDataChangeNotifier by instance<UserDataChangeNotifier>()
    routingV1 {
        authenticateFirebase {
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
