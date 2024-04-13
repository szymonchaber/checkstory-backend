package tech.checkstory.checklist.data

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.kotlin.datetime.datetime
import tech.checkstory.template.data.table.TemplateTable

object ChecklistTable : UUIDTable() {

    val template = reference("template_id", TemplateTable, ReferenceOption.CASCADE)
    val userId = varchar("user_id", length = 255)
    val notes = varchar("notes", length = 255)
    val createdAt = datetime("created_at")
    val isDeleted = bool("is_deleted")
}

