package tech.checkstory.template.domain.command

import kotlinx.serialization.Serializable
import tech.checkstory.core.domain.DtoUUID
import tech.checkstory.template.domain.aggregate.TemplateTask
import java.util.*

@Serializable
data class ExistingTemplateTaskDto(
    val id: DtoUUID,
    val parentId: DtoUUID?,
    val title: String,
    val children: List<ExistingTemplateTaskDto>,
    val sortPosition: Long,
) {

    fun toTemplateTask(templateId: UUID): TemplateTask {
        return TemplateTask(
            id = id,
            templateId = templateId,
            title = title,
            sortPosition = sortPosition,
            parentId = parentId,
            children = children.map { it.toTemplateTask(templateId) }
        )
    }
}
