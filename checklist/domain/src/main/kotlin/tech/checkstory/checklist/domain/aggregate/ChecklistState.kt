package tech.checkstory.checklist.domain.aggregate

import kotlinx.datetime.Instant
import tech.checkstory.core.domain.DtoUUID

sealed interface ChecklistState {

    data object NonExistent : ChecklistState

    data class Existing(
        val id: DtoUUID,
        val userId: String,
        val templateId: DtoUUID,
        val notes: String,
        val tasks: List<Task> = listOf(),
        val createdAt: Instant,
        val updatedAt: Instant,
        val isDeleted: Boolean = false
    ) : ChecklistState
}
