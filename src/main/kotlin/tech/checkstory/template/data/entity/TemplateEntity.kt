package tech.checkstory.template.data.entity

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.SizedIterable
import org.jetbrains.exposed.sql.SortOrder
import tech.checkstory.data.UpsertableUUIDEntityClass
import tech.checkstory.template.data.table.ReminderTable
import tech.checkstory.template.data.table.TemplateTable
import tech.checkstory.template.data.table.TemplateTaskTable
import tech.checkstory.template.domain.Template
import java.util.*

class TemplateEntity(id: EntityID<UUID>) : UUIDEntity(id) {

    companion object : UpsertableUUIDEntityClass<TemplateEntity>(TemplateTable)

    var title by TemplateTable.title
    var description by TemplateTable.description
    var userId by TemplateTable.userId
    var createdAt by TemplateTable.createdAt
    val tasks: SizedIterable<TemplateTaskEntity> by TemplateTaskEntity referrersOn TemplateTaskTable.template
    val reminders: SizedIterable<ReminderEntity> by ReminderEntity referrersOn ReminderTable.template

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
                .map(TemplateTaskEntity::toTemplateTask),
            reminders = reminders.map(ReminderEntity::toDomainReminder)
        )
    }
}
