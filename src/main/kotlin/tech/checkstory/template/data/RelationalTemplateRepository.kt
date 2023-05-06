package tech.checkstory.template.data

import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import tech.checkstory.auth.user.User
import tech.checkstory.template.data.RelationalTemplateRepository.Templates.id
import tech.checkstory.template.domain.TemplateRepository
import tech.checkstory.template.domain.aggregate.Template
import java.util.*

class RelationalTemplateRepository(private val database: Database) : TemplateRepository {

    object Templates : Table() {
        val id = uuid("id").autoGenerate()
        val userId = varchar("userId", length = 255)
        val name = varchar("name", length = 255)
        val description = varchar("description", length = 255)

        override val primaryKey = PrimaryKey(id)
    }

    init {
        transaction(database) {
            SchemaUtils.create(Templates)
        }
    }

    suspend fun <T> dbQuery(block: suspend () -> T): T {
        return newSuspendedTransaction(Dispatchers.IO) { block() }
    }

    suspend fun create(template: Template, user: User) {
        dbQuery {
            Templates.insert {
                it[id] = template.id
                it[userId] = user.userId
                it[name] = template.name
                it[description] = template.description
            }
        }
    }

    override suspend fun read(user: User, id: UUID): Template? {
        return dbQuery {
            Templates.select { Templates.id eq id and (Templates.userId eq user.userId) }
                .map {
                    Template(
                        id = it[Templates.id],
                        userId = "",
                        name = it[Templates.name],
                        description = it[Templates.description]
                    )
                }
                .singleOrNull()
        }
    }

    override suspend fun readAll(user: User): List<Template> {
        return dbQuery {
            Templates.select { Templates.userId eq user.userId }
                .map {
                    Template(
                        id = it[id],
                        userId = it[Templates.userId],
                        name = it[Templates.name],
                        description = it[Templates.description],
                    )
                }
        }
    }

    suspend fun updateTitle(user: User, templateId: UUID, newTitle: String) {
        dbQuery {
            Templates.update({ (id eq templateId) and (Templates.userId eq user.userId) }) {
                it[name] = newTitle
            }
        }
    }

    suspend fun delete(user: User, id: UUID) {
        dbQuery {
            Templates.deleteWhere { Templates.id eq id and (userId eq user.userId) }
        }
    }
}
