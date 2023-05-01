package tech.checkstory.checklist.domain.event

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import tech.checkstory.checklist.domain.aggregate.Checklist
import tech.checkstory.checklist.domain.aggregate.Task
import tech.checkstory.domain.command.DtoUUID
import tech.checkstory.domain.event.DomainEvent

@Serializable
sealed interface ChecklistEvent : DomainEvent {

    val checklistId: DtoUUID

    fun applyTo(checklist: Checklist): Checklist

    @Serializable
    @SerialName("createChecklist")
    data class CreateChecklistEvent(
        override val checklistId: DtoUUID,
        val templateId: DtoUUID,
        val title: String,
        val description: String,
        val tasks: List<Task>,
        override val eventId: DtoUUID,
        override val timestamp: Long,
        override val userId: String
    ) : ChecklistEvent {

        override val eventType: String = "createChecklist"

        override fun applyTo(checklist: Checklist): Checklist {
            return checklist.copy(
                id = checklistId,
                templateId = templateId,
                title = title,
                description = description,
                tasks = tasks,
                userId = userId
            )
        }
    }

    @Serializable
    @SerialName("editChecklistNotes")
    data class EditChecklistNotesEvent(
        override val checklistId: DtoUUID,
        val newNotes: String,
        override val eventId: DtoUUID,
        override val timestamp: Long,
        override val userId: String
    ) : ChecklistEvent {

        override val eventType: String = "editChecklistNotes"

        override fun applyTo(checklist: Checklist): Checklist {
            require(checklist.userId == userId)
            return checklist.copy(notes = newNotes)
        }
    }

    @Serializable
    @SerialName("changeTaskChecked")
    data class ChangeTaskCheckedEvent(
        override val checklistId: DtoUUID,
        val taskId: DtoUUID,
        val isChecked: Boolean,
        override val eventId: DtoUUID,
        override val timestamp: Long,
        override val userId: String
    ) : ChecklistEvent {

        override val eventType: String = "changeTaskChecked"

        override fun applyTo(checklist: Checklist): Checklist {
            require(checklist.userId == userId)
            val newTasks = checklist.tasks.map {
                it.withUpdatedIsCheckedRecursive(taskId, isChecked)
            }
            return checklist.copy(tasks = newTasks)
        }
    }

    @Serializable
    @SerialName("deleteChecklist")
    data class DeleteChecklistEvent(
        override val checklistId: DtoUUID,
        override val eventId: DtoUUID,
        override val timestamp: Long,
        override val userId: String
    ) : ChecklistEvent {

        override val eventType: String
            get() = "deleteChecklist"

        override fun applyTo(checklist: Checklist): Checklist {
            return checklist.copy(isDeleted = true)
        }
    }
}