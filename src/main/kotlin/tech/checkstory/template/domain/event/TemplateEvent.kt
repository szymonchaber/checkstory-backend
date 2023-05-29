package tech.checkstory.template.domain.event

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import tech.checkstory.domain.command.DtoUUID
import tech.checkstory.domain.event.CreationEvent
import tech.checkstory.domain.event.DomainEvent
import tech.checkstory.domain.event.MutationEvent
import tech.checkstory.template.domain.aggregate.TemplateTask
import tech.checkstory.template.domain.command.ReminderDto

@Serializable
sealed interface TemplateEvent : DomainEvent {

    @Serializable
    @SerialName("createTemplate")
    data class CreateTemplateEvent(
        override val eventId: DtoUUID,
        override val timestamp: Instant,
        val userId: String
    ) : TemplateEvent, CreationEvent {

        override val eventType: String = "createTemplate"
    }

    @Serializable
    @SerialName("editTemplateTitle")
    data class EditTemplateTitleEvent(
        val newTitle: String,
        override val eventId: DtoUUID,
        override val timestamp: Instant
    ) : TemplateEvent, MutationEvent {

        override val eventType: String = "editTemplateTitle"
    }

    @Serializable
    @SerialName("editTemplateDescription")
    data class EditTemplateDescriptionEvent(
        val newDescription: String,
        override val eventId: DtoUUID,
        override val timestamp: Instant
    ) : TemplateEvent, MutationEvent {

        override val eventType: String = "editTemplateDescription"
    }

    @Serializable
    @SerialName("addTemplateTask")
    data class AddTemplateTaskEvent(
        val taskId: DtoUUID,
        val parentTaskId: DtoUUID?,
        override val eventId: DtoUUID,
        override val timestamp: Instant
    ) : TemplateEvent, MutationEvent {

        override val eventType: String = "addTemplateTask"
    }

    @Serializable
    @SerialName("renameTemplateTask")
    data class RenameTemplateTaskEvent(
        val taskId: DtoUUID,
        val newTitle: String,
        override val eventId: DtoUUID,
        override val timestamp: Instant
    ) : TemplateEvent, MutationEvent {

        override val eventType: String = "renameTemplateTask"
    }

    @Serializable
    @SerialName("deleteTemplateTask")
    data class DeleteTemplateTaskEvent(
        val taskId: DtoUUID,
        override val eventId: DtoUUID,
        override val timestamp: Instant
    ) : TemplateEvent, MutationEvent {

        override val eventType: String = "deleteTemplateTask"
    }

    @Serializable
    @SerialName("updateTemplateTasksPositions")
    data class UpdateTasksPositionsEvent(
        val taskIdToLocalPosition: Map<DtoUUID, Long>,
        override val eventId: DtoUUID,
        override val timestamp: Instant
    ) : TemplateEvent, MutationEvent {

        override val eventType: String = "updateTemplateTasksPositions"
    }

    @Serializable
    @SerialName("moveTemplateTask")
    data class MoveTemplateTaskEvent(
        val taskId: DtoUUID,
        val newParentTaskId: DtoUUID?,
        override val eventId: DtoUUID,
        override val timestamp: Instant
    ) : TemplateEvent, MutationEvent {

        override val eventType: String = "moveTemplateTask"
    }

    @Serializable
    @SerialName("addOrUpdateTemplateReminder")
    data class AddOrUpdateReminderEvent(
        val reminder: ReminderDto,
        override val eventId: DtoUUID,
        override val timestamp: Instant
    ) : TemplateEvent, MutationEvent {

        override val eventType: String = "addOrUpdateTemplateReminder"
    }

    @Serializable
    @SerialName("deleteTemplateReminder")
    data class DeleteReminderEvent(
        val reminderId: DtoUUID,
        override val eventId: DtoUUID,
        override val timestamp: Instant
    ) : TemplateEvent, MutationEvent {

        override val eventType: String = "deleteTemplateReminder"
    }

    @Serializable
    @SerialName("deleteTemplate")
    data class DeleteTemplateEvent(
        override val eventId: DtoUUID,
        override val timestamp: Instant
    ) : TemplateEvent, MutationEvent {

        override val eventType: String = "deleteTemplate"
    }
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
