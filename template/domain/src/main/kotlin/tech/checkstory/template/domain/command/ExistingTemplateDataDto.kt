package tech.checkstory.template.domain.command

import kotlinx.serialization.Serializable

@Serializable
data class ExistingTemplateDataDto(
    val title: String,
    val description: String,
    val tasks: List<ExistingTemplateTaskDto>,
    val reminders: List<ReminderDto>,
)
