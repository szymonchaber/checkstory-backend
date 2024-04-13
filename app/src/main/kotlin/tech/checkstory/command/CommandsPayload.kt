package tech.checkstory.command

import kotlinx.serialization.Serializable
import tech.checkstory.checklist.domain.ChecklistCommand
import tech.checkstory.template.domain.command.TemplateCommand

@Serializable
data class CommandsPayload(
    val templateCommands: List<TemplateCommand>,
    val checklistCommands: List<ChecklistCommand>
)
