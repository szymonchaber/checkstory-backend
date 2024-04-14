package tech.checkstory.template.data.table

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption

object TemplateTaskTable : UUIDTable("template_task") {

    val title = varchar("title", 255)
    val sortPosition = long("sort_position").default(0)
    val template = reference("template_id", TemplateTable, ReferenceOption.CASCADE)
    val parent = optReference("parent_id", TemplateTaskTable, ReferenceOption.CASCADE)
    val isDeleted = bool("is_deleted")
}
