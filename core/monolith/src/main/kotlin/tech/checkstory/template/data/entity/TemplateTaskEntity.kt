package tech.checkstory.template.data.entity

import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.SizedIterable
import org.jetbrains.exposed.sql.SortOrder
import tech.checkstory.data.UpsertableUUIDEntityClass
import tech.checkstory.template.data.table.TemplateTaskTable
import tech.checkstory.template.domain.aggregate.TemplateTask
import java.util.*

class TemplateTaskEntity(id: EntityID<UUID>) : UUIDEntity(id) {

    companion object : UpsertableUUIDEntityClass<TemplateTaskEntity>(TemplateTaskTable)

    var title by TemplateTaskTable.title
    var template by TemplateTaskTable.template
    var parent by TemplateTaskTable.parent
    var sortPosition by TemplateTaskTable.sortPosition
    val children: SizedIterable<TemplateTaskEntity> by TemplateTaskEntity optionalReferrersOn TemplateTaskTable.parent

    fun toTemplateTask(): TemplateTask {
        return TemplateTask(
            id.value,
            template.value,
            title,
            sortPosition,
            parent?.value,
            children
                .orderBy(TemplateTaskTable.sortPosition to SortOrder.ASC)
                .map(TemplateTaskEntity::toTemplateTask),
        )
    }
}
