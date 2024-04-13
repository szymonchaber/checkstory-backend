package tech.checkstory.checklist.domain.aggregate

import kotlinx.serialization.Serializable
import tech.checkstory.core.domain.DtoUUID

@Serializable
data class Task(
    val id: DtoUUID,
    val checklistId: DtoUUID,
    val title: String,
    val sortPosition: Long,
    val isChecked: Boolean,
    val children: List<Task> = listOf()
) {

    fun withUpdatedIsCheckedRecursive(id: DtoUUID, isChecked: Boolean): Task {
        return if (this.id == id) {
            copy(isChecked = isChecked)
        } else {
            copy(children = children.map { it.withUpdatedIsCheckedRecursive(id, isChecked) })
        }
    }

}
