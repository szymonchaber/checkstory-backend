package tech.checkstory.template

import tech.checkstory.auth.user.AuthenticatedScope
import tech.checkstory.data.EventRepository
import tech.checkstory.template.data.EventSourcedTemplateRepository
import tech.checkstory.template.domain.command.TemplateCommand
import tech.checkstory.template.domain.event.TemplateEvent

context(AuthenticatedScope)
suspend fun handleTemplateCommands(
    commands: List<TemplateCommand>,
    templateEventRepository: EventRepository<TemplateEvent>,
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
