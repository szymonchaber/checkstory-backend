package tech.checkstory.template.data

import kotlinx.coroutines.Dispatchers
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.kotlin.datetime.datetime
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import tech.checkstory.auth.user.AuthenticatedScope
import tech.checkstory.template.domain.Template
import java.util.*

class RelationalTemplateRepository(private val database: Database) {

    object Templates : UUIDTable() {
        val userId = varchar("userId", length = 255)
        val name = varchar("name", length = 255)
        val description = varchar("description", length = 255)
        val createdAt = datetime("createdAt")
    }

    init {
        transaction(database) {
            SchemaUtils.create(Templates)
        }
    }

    suspend fun <T> dbQuery(block: suspend () -> T): T {
        return newSuspendedTransaction(Dispatchers.IO) { block() }
    }

    suspend fun create(template: Template) {
        dbQuery {
            Templates.insertIgnore {
                it[id] = template.id
                it[userId] = template.userId
                it[name] = template.name
                it[description] = template.description
                it[createdAt] = template.createdAt.toLocalDateTime(TimeZone.UTC)
            }
        }
    }

    context(AuthenticatedScope)
    suspend fun read(id: UUID): Template? {
        return dbQuery {
            Templates.select { Templates.id eq id and (Templates.userId eq principal.userId) }
                .map(::toTemplate)
                .singleOrNull()
        }
    }

    context(AuthenticatedScope)
    suspend fun readAll(): List<Template> {
        return dbQuery {
            Templates.select { Templates.userId eq principal.userId }
                .map(::toTemplate)
        }
    }

    private fun toTemplate(it: ResultRow) = Template(
        id = it[Templates.id].value,
        userId = it[Templates.userId],
        name = it[Templates.name],
        description = it[Templates.description],
        createdAt = it[Templates.createdAt].toInstant(TimeZone.UTC),
    )

    suspend fun delete(id: UUID) {
        dbQuery {
            Templates.deleteWhere { Templates.id eq id }
        }
    }
}
