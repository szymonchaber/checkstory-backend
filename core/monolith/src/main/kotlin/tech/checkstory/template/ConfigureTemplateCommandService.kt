package tech.checkstory.template

import tech.checkstory.core.domain.auth.AuthenticatedScope
import tech.checkstory.data.EventRepository
import tech.checkstory.template.data.TemplateAggregate
import tech.checkstory.template.domain.command.TemplateCommand
import tech.checkstory.template.domain.event.TemplateEvent

context(AuthenticatedScope)
suspend fun handleTemplateCommands(
    commands: List<TemplateCommand>,
    templateEventRepository: EventRepository<TemplateEvent>
) {
    commands
        .sortedBy { it.timestamp }
        .groupBy(TemplateCommand::templateId)
        .forEach { (templateId, commands) ->
            val events = TemplateAggregate.fromHistory(
                templateId,
                templateEventRepository.getEventsForAggregate(templateId)
            )
                .applyCommands(principal, commands)
            templateEventRepository.saveAll(events, templateId)
        }
}
