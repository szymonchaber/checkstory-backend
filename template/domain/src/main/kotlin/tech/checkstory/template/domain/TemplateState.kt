package tech.checkstory.template.domain

import kotlinx.datetime.Instant
import tech.checkstory.core.domain.DtoUUID
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
