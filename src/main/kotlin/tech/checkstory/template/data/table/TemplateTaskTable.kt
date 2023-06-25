package tech.checkstory.template.data.table

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption

object TemplateTaskTable : UUIDTable() {

    val title = varchar("title", 255)
    val sortPosition = long("sortPosition").default(0)
    val template = reference("templateId", TemplateTable, ReferenceOption.CASCADE)
    val parent = optReference("parent_id", TemplateTaskTable, ReferenceOption.CASCADE)
}
