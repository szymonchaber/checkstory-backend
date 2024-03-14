package tech.checkstory.template.data

import kotlinx.datetime.Instant
import tech.checkstory.core.domain.DtoUUID
import tech.checkstory.template.domain.aggregate.Reminder
import tech.checkstory.template.domain.aggregate.TemplateTask

sealed interface TemplateState {

    data object NonExistent : TemplateState

    data class Existing(
        val id: DtoUUID,
        val userId: String,
        val name: String,
        val description: String,
        val tasks: List<TemplateTask> = listOf(),
        val reminders: List<AggregateReminder> = listOf(),
        val createdAt: Instant,
        val isDeleted: Boolean = false
    ) : TemplateState
}

data class AggregateReminder(
    val reminder: Reminder,
    val isDeleted: Boolean
)
