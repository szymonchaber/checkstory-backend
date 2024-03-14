package tech.checkstory.template.domain.aggregate

import kotlinx.serialization.Serializable
import tech.checkstory.core.domain.DtoUUID

@Serializable
data class TemplateTask(
    val id: DtoUUID,
    val templateId: DtoUUID,
    val title: String,
    val sortPosition: Long,
    val parentId: DtoUUID?,
    val children: List<TemplateTask> = listOf()
)
