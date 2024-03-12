package tech.checkstory.command

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
import tech.checkstory.checklist.data.EventBasedChecklistRepository
import tech.checkstory.checklist.domain.event.ChecklistEvent
import tech.checkstory.data.EventRepository
import tech.checkstory.read.checklist.ChecklistProjection
import tech.checkstory.template.domain.event.TemplateEvent
import tech.checkstory.template.handleTemplateCommands

context(DI)
fun Application.configureSynchronization() {
    val templateEventRepository by instance<EventRepository<TemplateEvent>>()
    val checklistEventRepository by instance<EventRepository<ChecklistEvent>>()
    val checklistProjection by instance<ChecklistProjection>()
    val checklistRepository = EventBasedChecklistRepository(checklistEventRepository)
    routing {
        authenticate(FIREBASE_AUTH) {
            requirePayment()
            post<CommandsPayload>("/sync/commands") { (templateCommands, checklistCommands) ->
                withPrincipal {
                    withContext(Dispatchers.Default) {
                        handleTemplateCommands(
                            templateCommands,
                            templateEventRepository
                        )
                        handleChecklistCommands(
                            checklistCommands,
                            checklistRepository,
                            checklistEventRepository,
                            checklistProjection
                        )
                    }
                    call.respond<HttpStatusCode>(HttpStatusCode.NoContent)
                }
            }
        }
    }
}
