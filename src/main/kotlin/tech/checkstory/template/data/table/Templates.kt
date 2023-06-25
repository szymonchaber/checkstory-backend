package tech.checkstory.template.data.table

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.kotlin.datetime.datetime

object Templates : UUIDTable() {

    val userId = varchar("userId", length = 255)
    val title = varchar("title", length = 255)
    val description = varchar("description", length = 255)
    val createdAt = datetime("createdAt")
}
