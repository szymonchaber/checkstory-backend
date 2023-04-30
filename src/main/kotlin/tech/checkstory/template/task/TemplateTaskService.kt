package tech.checkstory.template.task

import kotlinx.coroutines.Dispatchers
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import tech.checkstory.firebase.user.FirebaseUser
import tech.checkstory.template.DtoUUID
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
                it[templateId] = templateTask.templateId
                it[name] = templateTask.title
            }
        }
    }

    suspend fun read(firebaseUser: FirebaseUser, id: UUID): TemplateTask? {
        return dbQuery {
            TemplateTasks.select { TemplateTasks.id eq id and (TemplateTasks.userId eq firebaseUser.userId) }
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

    suspend fun read(firebaseUser: FirebaseUser): List<TemplateTask> {
        return dbQuery {
            TemplateTasks.select { TemplateTasks.userId eq firebaseUser.userId }
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

    suspend fun getForTemplate(firebaseUser: FirebaseUser, templateId: UUID): List<TemplateTask> {
        return dbQuery {
            TemplateTasks.select { (TemplateTasks.templateId eq templateId) and (TemplateTasks.userId eq firebaseUser.userId) }
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
data class TemplateTask(
    val id: DtoUUID,
    val templateId: DtoUUID,
    val title: String,
    val sortPosition: Long,
    val children: List<TemplateTask> = listOf()
) {

    fun withUpdatedTitleRecursive(id: DtoUUID, newTitle: String): TemplateTask {
        return if (this.id == id) {
            copy(title = newTitle)
        } else {
            copy(children = children.map { it.withUpdatedTitleRecursive(id, newTitle) })
        }
    }
}

@Serializable
sealed interface Reminder {

    val id: DtoUUID
    val forTemplate: DtoUUID
    val startDateTime: LocalDateTime

    @Serializable
    @SerialName("exact")
    data class Exact(
        override val id: DtoUUID,
        override val forTemplate: DtoUUID,
        override val startDateTime: LocalDateTime
    ) : Reminder

    @Serializable
    @SerialName("recurring")
    data class Recurring(
        override val id: DtoUUID,
        override val forTemplate: DtoUUID,
        override val startDateTime: LocalDateTime,
        val interval: Interval
    ) : Reminder
}

@Serializable
sealed interface Interval {

    @Serializable
    @SerialName("daily")
    object Daily : Interval

    @Serializable
    @SerialName("weekly")
    data class Weekly(val dayOfWeek: DayOfWeek) : Interval

    @Serializable
    @SerialName("monthly")
    data class Monthly(val dayOfMonth: Int) : Interval

    @Serializable
    @SerialName("yearly")
    data class Yearly(val dayOfYear: Int) : Interval
}
