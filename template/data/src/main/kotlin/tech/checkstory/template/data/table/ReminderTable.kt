package tech.checkstory.template.data.table

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.kotlin.datetime.datetime

object ReminderTable : UUIDTable("reminder") {

    val template = reference("template_id", TemplateTable, ReferenceOption.CASCADE)
    val startDate = datetime("start_date")
    val recurrencePattern = varchar("recurrence_pattern", 255).nullable()
}
