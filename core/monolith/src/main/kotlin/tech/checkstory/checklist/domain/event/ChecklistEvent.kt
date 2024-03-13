package tech.checkstory.checklist.domain.event

import kotlinx.datetime.Instant
import tech.checkstory.checklist.domain.aggregate.Task
import tech.checkstory.core.domain.DtoUUID
import tech.checkstory.domain.event.DomainEvent

sealed interface ChecklistEvent : DomainEvent {

    data class CreateChecklistEvent(
        val templateId: DtoUUID,
        val title: String,
        val description: String,
        val tasks: List<Task>,
        val userId: String,
        val notes: String?,
        override val aggregateId: DtoUUID,
        override val timestamp: Instant,
    ) : ChecklistEvent

    data class EditChecklistNotesEvent(
        val newNotes: String,
        override val aggregateId: DtoUUID,
        override val timestamp: Instant,
    ) : ChecklistEvent

    data class ChangeTaskCheckedEvent(
        val taskId: DtoUUID,
        val isChecked: Boolean,
        override val aggregateId: DtoUUID,
        override val timestamp: Instant,
    ) : ChecklistEvent

    data class DeleteChecklistEvent(
        override val aggregateId: DtoUUID,
        override val timestamp: Instant,
    ) : ChecklistEvent
}
