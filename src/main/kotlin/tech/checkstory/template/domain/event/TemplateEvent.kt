package tech.checkstory.template.domain.event

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import tech.checkstory.domain.command.DtoUUID
import tech.checkstory.domain.event.DomainEvent
import tech.checkstory.template.domain.aggregate.TemplateTask
import tech.checkstory.template.domain.command.ReminderDto

@Serializable
sealed interface TemplateEvent : DomainEvent {

    val templateId: DtoUUID

    @Serializable
    @SerialName("createTemplate")
    data class CreateTemplateEvent(
        override val templateId: DtoUUID,
        override val eventId: DtoUUID,
        override val timestamp: Long,
        override val userId: String
    ) : TemplateEvent {

        override val eventType: String = "createTemplate"

    }

    @Serializable
    @SerialName("editTemplateTitle")
    data class EditTemplateTitleEvent(
        override val templateId: DtoUUID,
        val newTitle: String,
        override val eventId: DtoUUID,
        override val timestamp: Long,
        override val userId: String
    ) : TemplateEvent {

        override val eventType: String = "editTemplateTitle"

    }

    @Serializable
    @SerialName("editTemplateDescription")
    data class EditTemplateDescriptionEvent(
        override val templateId: DtoUUID,
        val newDescription: String,
        override val eventId: DtoUUID,
        override val timestamp: Long,
        override val userId: String
    ) : TemplateEvent {

        override val eventType: String = "editTemplateDescription"

    }

    @Serializable
    @SerialName("addTemplateTask")
    data class AddTemplateTaskEvent(
        override val templateId: DtoUUID,
        val taskId: DtoUUID,
        val parentTaskId: DtoUUID?,
        override val eventId: DtoUUID,
        override val timestamp: Long,
        override val userId: String
    ) : TemplateEvent {

        override val eventType: String = "addTemplateTask"

    }

    @Serializable
    @SerialName("renameTemplateTask")
    data class RenameTemplateTaskEvent(
        override val templateId: DtoUUID,
        val taskId: DtoUUID,
        val newTitle: String,
        override val eventId: DtoUUID,
        override val timestamp: Long,
        override val userId: String
    ) : TemplateEvent {

        override val eventType: String = "renameTemplateTask"

    }

    @Serializable
    @SerialName("deleteTemplateTask")
    data class DeleteTemplateTaskEvent(
        override val templateId: DtoUUID,
        val taskId: DtoUUID,
        override val eventId: DtoUUID,
        override val timestamp: Long,
        override val userId: String
    ) : TemplateEvent {

        override val eventType: String = "deleteTemplateTask"

    }

    @Serializable
    @SerialName("updateTemplateTasksPositions")
    data class UpdateTasksPositionsEvent(
        override val templateId: DtoUUID,
        val taskIdToLocalPosition: Map<DtoUUID, Long>,
        override val eventId: DtoUUID,
        override val timestamp: Long,
        override val userId: String
    ) : TemplateEvent {

        override val eventType: String = "updateTemplateTasksPositions"

    }

    @Serializable
    @SerialName("moveTemplateTask")
    data class MoveTemplateTaskEvent(
        override val templateId: DtoUUID,
        val taskId: DtoUUID,
        val newParentTaskId: DtoUUID?,
        override val eventId: DtoUUID,
        override val timestamp: Long,
        override val userId: String
    ) : TemplateEvent {
        override val eventType: String = "moveTemplateTask"

    }

    @Serializable
    @SerialName("addOrUpdateTemplateReminder")
    data class AddOrUpdateReminderEvent(
        override val templateId: DtoUUID,
        val reminder: ReminderDto,
        override val eventId: DtoUUID,
        override val timestamp: Long,
        override val userId: String
    ) : TemplateEvent {

        override val eventType: String
            get() = "addOrUpdateTemplateReminder"

    }

    @Serializable
    @SerialName("deleteTemplateReminder")
    data class DeleteReminderEvent(
        override val templateId: DtoUUID,
        val reminderId: DtoUUID,
        override val eventId: DtoUUID,
        override val timestamp: Long,
        override val userId: String
    ) : TemplateEvent {

        override val eventType: String
            get() = "deleteTemplateReminder"

    }

    @Serializable
    @SerialName("deleteTemplate")
    data class DeleteTemplateEvent(
        override val templateId: DtoUUID,
        override val eventId: DtoUUID,
        override val timestamp: Long,
        override val userId: String
    ) : TemplateEvent {

        override val eventType: String
            get() = "deleteTemplate"

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
