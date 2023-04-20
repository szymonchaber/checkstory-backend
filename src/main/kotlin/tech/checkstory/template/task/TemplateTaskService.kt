package tech.checkstory.template.task

import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import tech.checkstory.firebase.user.FirebaseUser
import java.util.*

class TemplateTaskService(private val database: Database) {

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

    suspend fun create(newId: UUID, templateTask: TemplateTask, firebaseUser: FirebaseUser) {
        dbQuery {
            TemplateTasks.insert {
                it[id] = newId
                it[userId] = firebaseUser.userId
                it[templateId] = UUID.fromString(templateTask.templateId)
                it[name] = templateTask.name
            }
        }
    }

    suspend fun read(firebaseUser: FirebaseUser, id: UUID): TemplateTask? {
        return dbQuery {
            TemplateTasks.select { TemplateTasks.id eq id and (TemplateTasks.userId eq firebaseUser.userId) }
                .map {
                    TemplateTask(
                        id = it[TemplateTasks.id].toString(),
                        templateId = it[TemplateTasks.templateId].toString(),
                        name = it[TemplateTasks.name]
                    )
                }
                .singleOrNull()
        }
    }

    suspend fun read(firebaseUser: FirebaseUser): List<TemplateTask> {
        return dbQuery {
            TemplateTasks.select { TemplateTasks.userId eq firebaseUser.userId }
                .map {
                    TemplateTask(
                        id = it[TemplateTasks.id].toString(),
                        templateId = it[TemplateTasks.templateId].toString(),
                        name = it[TemplateTasks.name]
                    )
                }
        }
    }

    suspend fun getForTemplate(firebaseUser: FirebaseUser, templateId: UUID): List<TemplateTask> {
        return dbQuery {
            TemplateTasks.select { (TemplateTasks.templateId eq templateId) and (TemplateTasks.userId eq firebaseUser.userId) }
                .map {
                    TemplateTask(
                        id = it[TemplateTasks.id].toString(),
                        templateId = it[TemplateTasks.templateId].toString(),
                        name = it[TemplateTasks.name]
                    )
                }
        }
    }

    suspend fun updateTitle(firebaseUser: FirebaseUser, templateId: UUID, newTitle: String) {
        dbQuery {
            TemplateTasks.update({ (TemplateTasks.id eq templateId) and (TemplateTasks.userId eq firebaseUser.userId) }) {
                it[name] = newTitle
            }
        }
    }

    suspend fun delete(firebaseUser: FirebaseUser, id: UUID) {
        dbQuery {
            TemplateTasks.deleteWhere { TemplateTasks.id eq id and (userId eq firebaseUser.userId) }
        }
    }
}

@Serializable
data class TemplateTask(val id: String, val templateId: String, val name: String)
