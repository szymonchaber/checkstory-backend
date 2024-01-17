package tech.checkstory.sync

import kotlinx.serialization.Serializable
import tech.checkstory.checklist.domain.command.ChecklistCommand
import tech.checkstory.template.domain.command.TemplateCommand

@Serializable
data class CommandsPayload(
    val templateCommands: List<TemplateCommand>,
    val checklistCommands: List<ChecklistCommand>
)
