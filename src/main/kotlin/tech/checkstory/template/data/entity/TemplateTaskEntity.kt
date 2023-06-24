package tech.checkstory.template.data.entity

import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.SizedIterable
import tech.checkstory.template.data.table.TemplateTaskTable
import tech.checkstory.template.domain.aggregate.TemplateTask
import java.util.*

class TemplateTaskEntity(id: EntityID<UUID>) : UUIDEntity(id) {

    companion object : UUIDEntityClass<TemplateTaskEntity>(TemplateTaskTable)

    var title by TemplateTaskTable.title
    var template by TemplateTaskTable.template
    var parent by TemplateTaskTable.parent
    val children: SizedIterable<TemplateTaskEntity> by TemplateTaskEntity optionalReferrersOn TemplateTaskTable.parent

    fun toTemplateTask(): TemplateTask {
        return TemplateTask(
            id.value,
            template.value,
            title,
            0,
            children.map {
                it.toTemplateTask()
            }
        )
    }
}
