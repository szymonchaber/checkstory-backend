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
import tech.checkstory.checklist.domain.ChecklistRepository
import tech.checkstory.command.CommandsPayload
import tech.checkstory.core.domain.auth.AuthenticatedScope
import tech.checkstory.data.EventRepository
import tech.checkstory.template.data.RelationalTemplateRepository
import tech.checkstory.template.domain.aggregate.Template
import tech.checkstory.template.domain.command.TemplateCommand
import tech.checkstory.template.domain.event.TemplateEvent

context(DI)
fun Application.configureTemplate() {
    val templateService by instance<RelationalTemplateRepository>()
    routing {
        authenticate(FIREBASE_AUTH) {
            get("/templates") {
                withPrincipal {
                    val templates = templateService.readAllNotDeleted()
                    call.respond<List<Template>>(HttpStatusCode.OK, templates)
                }
            }
        }
    }
}


context(AuthenticatedScope, DI)
suspend fun handleTemplateCommands(
    commands: List<TemplateCommand>,
    templateEventRepository: EventRepository<TemplateEvent>,
) {
    val templateRepository by instance<RelationalTemplateRepository>()

    commands
        .sortedBy { it.timestamp }
        .groupBy(tech.checkstory.template.domain.command.TemplateCommand::templateId)
        .forEach { (templateId, commands) ->
            val events = tech.checkstory.template.data.TemplateAggregate.fromHistory(
                templateId,
                templateEventRepository.getEventsForAggregate(templateId)
            )
                .applyCommands(principal, commands)
            templateEventRepository.saveAll(events, templateId)
        }
}


context(DI)
fun Application.configureSynchronization() {
    val templateEventRepository by instance<EventRepository<TemplateEvent>>()
    val checklistRepository by instance<ChecklistRepository>()
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
                            checklistRepository
                        )
                    }
                    call.respond<HttpStatusCode>(HttpStatusCode.NoContent)
                }
            }
        }
    }
}
