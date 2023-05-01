package tech.checkstory.template.domain.aggregate

import kotlinx.serialization.Serializable
import tech.checkstory.domain.command.DtoUUID

@Serializable
data class TemplateTask(
    val id: DtoUUID,
    val templateId: DtoUUID,
    val title: String,
    val sortPosition: Long,
    val children: List<TemplateTask> = listOf()
) {

    fun withUpdatedTitleRecursive(id: DtoUUID, newTitle: String): TemplateTask {
        return if (this.id == id) {
            copy(title = newTitle)
        } else {
            copy(children = children.map { it.withUpdatedTitleRecursive(id, newTitle) })
        }
    }
}