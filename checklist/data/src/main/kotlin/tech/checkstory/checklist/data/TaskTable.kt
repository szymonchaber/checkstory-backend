package tech.checkstory.checklist.data

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption

object TaskTable : UUIDTable("task") {

    val title = varchar("title", 255)
    val checklist = reference("checklist_id", ChecklistTable, ReferenceOption.CASCADE)
    val parent = optReference("parent_id", TaskTable, ReferenceOption.CASCADE)
    val sortPosition = long("sort_position").default(0)
    val isChecked = bool("is_checked")
}
