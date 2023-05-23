package tech.checkstory.template.domain.aggregate

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import tech.checkstory.domain.command.DtoUUID

@Serializable
data class Template(
    val id: DtoUUID,
    val userId: String,
    val name: String,
    val description: String,
    val tasks: List<TemplateTask> = listOf(),
    val reminders: List<Reminder> = listOf(),
    val createdAt: Instant,
    val isDeleted: Boolean = false
)