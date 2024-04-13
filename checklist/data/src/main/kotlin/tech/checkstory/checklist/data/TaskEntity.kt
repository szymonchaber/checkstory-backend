package tech.checkstory.checklist.data

import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.SizedIterable
import org.jetbrains.exposed.sql.SortOrder
import tech.checkstory.checklist.domain.aggregate.Task
import tech.checkstory.data.UpsertableUUIDEntityClass
import java.util.*

class TaskEntity(id: EntityID<UUID>) : UUIDEntity(id) {

    companion object : UpsertableUUIDEntityClass<TaskEntity>(TaskTable)

    var title by TaskTable.title
    var checklistId by TaskTable.checklist
    var parent by TaskTable.parent
    var sortPosition by TaskTable.sortPosition
    val children: SizedIterable<TaskEntity> by TaskEntity optionalReferrersOn TaskTable.parent

    var isChecked by TaskTable.isChecked

    fun toTask(): Task {
        return Task(
            id.value,
            checklistId.value,
            title,
            sortPosition,
            isChecked,
            children
                .orderBy(TaskTable.sortPosition to SortOrder.ASC)
                .map(TaskEntity::toTask)
        )
    }
}
