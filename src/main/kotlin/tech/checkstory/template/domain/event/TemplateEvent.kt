package tech.checkstory.template.domain.event

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import tech.checkstory.domain.command.DtoUUID
import tech.checkstory.domain.event.DomainEvent
import tech.checkstory.template.domain.aggregate.Template
import tech.checkstory.template.domain.aggregate.TemplateTask
import tech.checkstory.template.domain.command.ReminderDto

@Serializable
sealed interface TemplateEvent : DomainEvent {

    val templateId: DtoUUID

    fun applyTo(template: Template): Template

    @Serializable
    @SerialName("createTemplate")
    data class CreateTemplateEvent(
        override val templateId: DtoUUID,
        override val eventId: DtoUUID,
        override val timestamp: Long,
        override val userId: String
    ) : TemplateEvent {

        override val eventType: String = "createTemplate"

        override fun applyTo(template: Template): Template {
            return template.copy(userId = userId, id = templateId)
        }
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

        override fun applyTo(template: Template): Template {
            require(template.userId == userId)
            return template.copy(name = newTitle)
        }
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

        override fun applyTo(template: Template): Template {
            require(template.userId == userId)
            return template.copy(description = newDescription)
        }
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

        override fun applyTo(template: Template): Template {
            require(template.userId == userId)
            val newItem = TemplateTask(taskId, templateId, "", template.tasks.size.toLong())
            val items = if (parentTaskId == null) {
                template.tasks.plus(newItem)
            } else {
                template.tasks.map { it.withMovedChildRecursive(parentTaskId, newItem) }
            }
            return template.copy(tasks = items)
        }
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

        override fun applyTo(template: Template): Template {
            require(template.userId == userId)
            val newTasks = template.tasks.map {
                it.withUpdatedTitleRecursive(taskId, newTitle)
            }
            return template.copy(tasks = newTasks)
        }
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

        override fun applyTo(template: Template): Template {
            require(template.userId == userId)
            val (filteredTasks, _) = template.tasks.withExtractedTask(taskId)
            return template.copy(tasks = filteredTasks)
        }
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

        override fun applyTo(template: Template): Template {
            return template.copy(
                tasks = template.tasks.map(::updatePosition).sortedBy(TemplateTask::sortPosition)
            )
        }

        private fun updatePosition(templateCheckbox: TemplateTask): TemplateTask {
            val newPosition = taskIdToLocalPosition[templateCheckbox.id] ?: templateCheckbox.sortPosition
            return templateCheckbox
                .copy(
                    sortPosition = newPosition,
                    children = templateCheckbox.children.map(::updatePosition).sortedBy(TemplateTask::sortPosition)
                )
        }
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

        override fun applyTo(template: Template): Template {
            val (filteredTasks, movedItem) = template.tasks.withExtractedTask(taskId)
            val items = if (newParentTaskId == null) {
                filteredTasks.plus(movedItem)
            } else {
                filteredTasks.map { it.withMovedChildRecursive(newParentTaskId, movedItem) }
            }
            return template.copy(tasks = items)
        }
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

        override fun applyTo(template: Template): Template {
            val reminder = reminder.toReminder()
            val newReminders = if (template.reminders.any { it.id == reminder.id }) {
                template.reminders.map {
                    if (it.id == reminder.id) {
                        reminder
                    } else {
                        it
                    }
                }
            } else {
                template.reminders.plus(reminder)
            }
            return template.copy(reminders = newReminders)
        }
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

        override fun applyTo(template: Template): Template {
            return template.copy(reminders = template.reminders.filterNot { it.id == reminderId })
        }
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

        override fun applyTo(template: Template): Template {
            return template.copy(isDeleted = true)
        }
    }
}


private fun List<TemplateTask>.withExtractedTask(id: DtoUUID): Pair<List<TemplateTask>, TemplateTask> {
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

private fun TemplateTask.withoutChild(
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
