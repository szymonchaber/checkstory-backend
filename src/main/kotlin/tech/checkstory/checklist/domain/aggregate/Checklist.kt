package tech.checkstory.checklist.domain.aggregate

import kotlinx.serialization.Serializable
import tech.checkstory.template.DtoUUID

@Serializable
data class Checklist(
    val id: DtoUUID,
    val userId: String,
    val templateId: DtoUUID,
    val title: String,
    val description: String,
    val notes: String,
    val tasks: List<Task> = listOf(),
    val isDeleted: Boolean = false
)