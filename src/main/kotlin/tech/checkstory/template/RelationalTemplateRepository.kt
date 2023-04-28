package tech.checkstory.template

import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import tech.checkstory.firebase.user.FirebaseUser
import tech.checkstory.template.RelationalTemplateRepository.Templates.id
import tech.checkstory.template.task.Reminder
import tech.checkstory.template.task.TemplateTask
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

    suspend fun create(template: Template, firebaseUser: FirebaseUser) {
        dbQuery {
            Templates.insert {
                it[id] = UUID.fromString(template.id)
                it[userId] = firebaseUser.userId
                it[name] = template.name
                it[description] = template.description
            }
        }
    }

    override suspend fun read(firebaseUser: FirebaseUser, id: UUID): Template? {
        return dbQuery {
            Templates.select { Templates.id eq id and (Templates.userId eq firebaseUser.userId) }
                .map {
                    Template(
                        id = it[Templates.id].toString(),
                        userId = "",
                        name = it[Templates.name],
                        description = it[Templates.description]
                    )
                }
                .singleOrNull()
        }
    }

    override suspend fun readAll(firebaseUser: FirebaseUser): List<Template> {
        return dbQuery {
            Templates.select { Templates.userId eq firebaseUser.userId }
                .map {
                    Template(
                        id = it[Templates.id].toString(),
                        userId = "",
                        name = it[Templates.name],
                        description = it[Templates.description],
                    )
                }
        }
    }

    suspend fun updateTitle(firebaseUser: FirebaseUser, templateId: UUID, newTitle: String) {
        dbQuery {
            Templates.update({ (id eq templateId) and (Templates.userId eq firebaseUser.userId) }) {
                it[name] = newTitle
            }
        }
    }

    suspend fun delete(firebaseUser: FirebaseUser, id: UUID) {
        dbQuery {
            Templates.deleteWhere { Templates.id eq id and (userId eq firebaseUser.userId) }
        }
    }
}

@Serializable
data class Template(
    val id: String,
    val userId: String,
    val name: String,
    val description: String,
    val tasks: List<TemplateTask> = listOf(),
    val reminders: List<Reminder> = listOf()
)
