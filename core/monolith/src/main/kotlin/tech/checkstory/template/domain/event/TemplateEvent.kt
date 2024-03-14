package tech.checkstory.template.domain.event

import kotlinx.datetime.Instant
import tech.checkstory.core.domain.DtoUUID
import tech.checkstory.domain.event.DomainEvent
import tech.checkstory.template.domain.aggregate.TemplateTask
import tech.checkstory.template.domain.command.ExistingTemplateDataDto
import tech.checkstory.template.domain.command.ReminderDto

sealed interface TemplateEvent : DomainEvent {

    data class CreateTemplateEvent(
        override val aggregateId: DtoUUID,
        override val timestamp: Instant,
        val userId: String,
        val existingTemplateData: ExistingTemplateDataDto?
    ) : TemplateEvent

    data class AddTemplateTaskEvent(
        val taskId: DtoUUID,
        val parentTaskId: DtoUUID?,
        override val aggregateId: DtoUUID,
        override val timestamp: Instant
    ) : TemplateEvent

    data class RenameTemplateTaskEvent(
        val taskId: DtoUUID,
        val newTitle: String,
        override val aggregateId: DtoUUID,
        override val timestamp: Instant
    ) : TemplateEvent

    data class DeleteTemplateTaskEvent(
        val taskId: DtoUUID,
        override val aggregateId: DtoUUID,
        override val timestamp: Instant
    ) : TemplateEvent

    data class UpdateTasksPositionsEvent(
        val taskIdToLocalPosition: Map<DtoUUID, Long>,
        override val aggregateId: DtoUUID,
        override val timestamp: Instant
    ) : TemplateEvent

    data class MoveTemplateTaskEvent(
        val taskId: DtoUUID,
        val newParentTaskId: DtoUUID?,
        override val aggregateId: DtoUUID,
        override val timestamp: Instant
    ) : TemplateEvent

    data class AddOrUpdateReminderEvent(
        val reminder: ReminderDto,
        override val aggregateId: DtoUUID,
        override val timestamp: Instant
    ) : TemplateEvent

    data class DeleteReminderEvent(
        val reminderId: DtoUUID,
        override val aggregateId: DtoUUID,
        override val timestamp: Instant
    ) : TemplateEvent
}


fun List<TemplateTask>.withExtractedTask(id: DtoUUID): Pair<List<TemplateTask>, TemplateTask> {
    var movedItem: TemplateTask? = null
    val onItemFoundAndRemoved: (TemplateTask) -> Unit = {
        movedItem = it
    }
    val withExtractedElement = this
        .filter {
            if (it.id == id) {
                movedItem = it
                false
            } else {
                true
            }
        }
        .map {
            it.withoutChild(id, onItemFoundAndRemoved)
        }
    return withExtractedElement to movedItem!!
}

fun TemplateTask.withoutChild(
    childTaskId: DtoUUID,
    onItemFoundAndRemoved: (TemplateTask) -> Unit
): TemplateTask {
    val updatedChildren = children
        .firstOrNull {
            it.id == childTaskId
        }
        ?.let {
            onItemFoundAndRemoved(it)
            children.minus(it)
        }
        ?: children.map {
            it.withoutChild(childTaskId, onItemFoundAndRemoved)
        }
    return copy(children = updatedChildren)
}


fun TemplateTask.withMovedChildRecursive(
    parentTask: DtoUUID,
    childTask: TemplateTask
): TemplateTask {
    val updatedChildren = if (id == parentTask) {
        children.plus(childTask)
    } else {
        children.map { it.withMovedChildRecursive(parentTask, childTask) }
    }
    return copy(children = updatedChildren)
}
