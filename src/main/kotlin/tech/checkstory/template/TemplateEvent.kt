package tech.checkstory.template

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import tech.checkstory.template.task.TemplateTask

@Serializable
sealed interface TemplateEvent : DomainEvent {

    val templateId: String

    fun applyTo(template: Template): Template

    @Serializable
    @SerialName("createTemplate")
    data class CreateTemplateEvent(
        override val templateId: String,
        override val eventId: String,
        override val timestamp: Long,
        override val userId: String
    ) : TemplateEvent {

        override val eventType: String = "createTemplate"

        override fun applyTo(template: Template): Template {
            return template.copy(userId = userId)
        }
    }

    @Serializable
    @SerialName("editTemplateTitle")
    data class EditTemplateTitleEvent(
        override val templateId: String,
        val newTitle: String,
        override val eventId: String,
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
        override val templateId: String,
        val newDescription: String,
        override val eventId: String,
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
        override val templateId: String,
        val taskId: String,
        val parentTaskId: String?,
        override val eventId: String,
        override val timestamp: Long,
        override val userId: String
    ) : TemplateEvent {

        override val eventType: String = "addTemplateTask"

        override fun applyTo(template: Template): Template {
            require(template.userId == userId)
            return template.copy(
                tasks = template.tasks.plus(TemplateTask(taskId, templateId, "", template.tasks.size.toLong()))
            )
        }
    }

    @Serializable
    @SerialName("renameTemplateTask")
    data class RenameTemplateTaskEvent(
        override val templateId: String,
        val taskId: String,
        val newTitle: String,
        override val eventId: String,
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
        override val templateId: String,
        val taskId: String,
        override val eventId: String,
        override val timestamp: Long,
        override val userId: String
    ) : TemplateEvent {

        override val eventType: String = "deleteTemplateTask"


        override fun applyTo(template: Template): Template {
            require(template.userId == userId)
            return template.copy(tasks = template.tasks.filterNot { it.id == taskId })
        }
    }

    @Serializable
    @SerialName("updateTemplateTasksPositions")
    data class UpdateTasksPositionsEvent(
        override val templateId: String,
        val taskIdToLocalPosition: Map<String, Long>,
        override val eventId: String,
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
}