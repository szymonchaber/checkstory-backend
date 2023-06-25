package tech.checkstory.template.data.entity

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.SizedIterable
import org.jetbrains.exposed.sql.SortOrder
import tech.checkstory.template.data.table.TemplateTaskTable
import tech.checkstory.template.data.table.Templates
import tech.checkstory.template.domain.Template
import java.util.*

class TemplateEntity(id: EntityID<UUID>) : UUIDEntity(id) {

    companion object : UUIDEntityClass<TemplateEntity>(Templates)

    var title by Templates.title
    var description by Templates.description
    var userId by Templates.userId
    var createdAt by Templates.createdAt
    val tasks: SizedIterable<TemplateTaskEntity> by TemplateTaskEntity referrersOn TemplateTaskTable.template

    fun toTemplate(): Template {
        return Template(
            id = id.value,
            userId = userId,
            name = title,
            description = description,
            createdAt = createdAt.toInstant(TimeZone.UTC),
            tasks = tasks
                .orderBy(TemplateTaskTable.sortPosition to SortOrder.ASC)
                .filter {
                    it.parent == null
                }
                .map(TemplateTaskEntity::toTemplateTask)
        )
    }
}
