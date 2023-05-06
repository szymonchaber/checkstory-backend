package tech.checkstory.template.data

import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import tech.checkstory.auth.user.User
import tech.checkstory.template.domain.aggregate.TemplateTask
import java.util.*

class TemplateTaskRepository(private val database: Database) {

    object TemplateTasks : Table() {
        val id = uuid("id").autoGenerate()
        val userId = varchar("userId", length = 255)
        val templateId = uuid("template_id")
        val name = varchar("name", length = 255)

        override val primaryKey = PrimaryKey(id)
    }

    init {
        transaction(database) {
            SchemaUtils.create(TemplateTasks)
        }
    }

    suspend fun <T> dbQuery(block: suspend () -> T): T {
        return newSuspendedTransaction(Dispatchers.IO) { block() }
    }

    suspend fun create(newId: UUID, templateTask: TemplateTask, user: User) {
        dbQuery {
            TemplateTasks.insert {
                it[id] = newId
                it[userId] = user.userId
                it[templateId] = templateTask.templateId
                it[name] = templateTask.title
            }
        }
    }

    suspend fun read(user: User, id: UUID): TemplateTask? {
        return dbQuery {
            TemplateTasks.select { TemplateTasks.id eq id and (TemplateTasks.userId eq user.userId) }
                .map {
                    TemplateTask(
                        id = it[TemplateTasks.id],
                        templateId = it[TemplateTasks.templateId],
                        title = it[TemplateTasks.name],
                        sortPosition = 0
                    )
                }
                .singleOrNull()
        }
    }

    suspend fun read(user: User): List<TemplateTask> {
        return dbQuery {
            TemplateTasks.select { TemplateTasks.userId eq user.userId }
                .map {
                    TemplateTask(
                        id = it[TemplateTasks.id],
                        templateId = it[TemplateTasks.templateId],
                        title = it[TemplateTasks.name],
                        sortPosition = 0
                    )
                }
        }
    }

    suspend fun getForTemplate(user: User, templateId: UUID): List<TemplateTask> {
        return dbQuery {
            TemplateTasks.select { (TemplateTasks.templateId eq templateId) and (TemplateTasks.userId eq user.userId) }
                .map {
                    TemplateTask(
                        id = it[TemplateTasks.id],
                        templateId = it[TemplateTasks.templateId],
                        title = it[TemplateTasks.name],
                        sortPosition = 0
                    )
                }
        }
    }

    suspend fun updateTitle(user: User, templateId: UUID, newTitle: String) {
        dbQuery {
            TemplateTasks.update({ (TemplateTasks.id eq templateId) and (TemplateTasks.userId eq user.userId) }) {
                it[name] = newTitle
            }
        }
    }

    suspend fun delete(user: User, id: UUID) {
        dbQuery {
            TemplateTasks.deleteWhere { TemplateTasks.id eq id and (userId eq user.userId) }
        }
    }
}
