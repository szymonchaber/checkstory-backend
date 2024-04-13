package tech.checkstory.template.module

import org.kodein.di.DI
import org.kodein.di.instance
import tech.checkstory.core.domain.auth.AuthenticatedScope
import tech.checkstory.template.data.TemplateRepository
import tech.checkstory.template.domain.TemplateAggregate
import tech.checkstory.template.domain.TemplateState
import tech.checkstory.template.domain.command.TemplateCommand

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
