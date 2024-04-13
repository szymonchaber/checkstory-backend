package tech.checkstory.checklist.data

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.SizedIterable
import org.jetbrains.exposed.sql.SortOrder
import tech.checkstory.checklist.domain.aggregate.Checklist
import tech.checkstory.data.UpsertableUUIDEntityClass
import tech.checkstory.template.data.entity.TemplateEntity
import java.util.*

class ChecklistEntity(id: EntityID<UUID>) : UUIDEntity(id) {

    companion object : UpsertableUUIDEntityClass<ChecklistEntity>(ChecklistTable)

    var notes by ChecklistTable.notes
    var userId by ChecklistTable.userId
    var createdAt by ChecklistTable.createdAt
    var templateId by ChecklistTable.template
    var isDeleted by ChecklistTable.isDeleted
    val tasks: SizedIterable<TaskEntity> by TaskEntity referrersOn TaskTable.checklist
    val template by TemplateEntity referencedOn ChecklistTable.template

    fun toChecklist(): Checklist {
        return Checklist(
            id = id.value,
            userId = userId,
            title = template.title,
            description = template.description,
            createdAt = createdAt.toInstant(TimeZone.UTC),
            notes = notes,
            templateId = templateId.value,
            tasks = tasks
                .orderBy(TaskTable.sortPosition to SortOrder.ASC)
                .filter {
                    it.parent == null
                }
                .map(TaskEntity::toTask),
            isDeleted = isDeleted
        )
    }
}
