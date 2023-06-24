package tech.checkstory.template.data

import kotlinx.coroutines.Dispatchers
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.kotlin.datetime.datetime
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import tech.checkstory.auth.user.AuthenticatedScope
import tech.checkstory.template.domain.Template
import tech.checkstory.template.domain.aggregate.TemplateTask
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
            SchemaUtils.create(TemplateTaskTable)
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
            template.tasks.forEach {
                insertTask(it, null)
            }
        }
    }

    fun insertTask(templateTask: TemplateTask, parent: TemplateTaskEntity?) {
        val entity = TemplateTaskEntity.new(templateTask.id) {
            title = templateTask.title
            this.parent = parent?.id
            this.template = EntityID(templateTask.templateId, Templates)
        }
        templateTask.children.forEach {
            insertTask(it, entity)
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

    context(AuthenticatedScope)
    suspend fun readTasks(): List<TemplateTask> {
        return dbQuery {
            TemplateTaskEntity.find {
                TemplateTaskTable.parent eq null
            }
                .map {
                    toTemplateTask(it)
                }
        }
    }

    private fun toTemplateTask(entity: TemplateTaskEntity): TemplateTask {
        return TemplateTask(
            entity.id.value,
            entity.template.value,
            entity.title,
            0,
            entity.children.map(::toTemplateTask)
        )
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

object TemplateTaskTable : UUIDTable() {

    val title = varchar("title", 255)
    val template = reference("templateId", RelationalTemplateRepository.Templates, ReferenceOption.CASCADE)
    val parent = optReference("parent_id", TemplateTaskTable, ReferenceOption.SET_NULL)
}

class TemplateTaskEntity(id: EntityID<UUID>) : UUIDEntity(id) {

    companion object : UUIDEntityClass<TemplateTaskEntity>(TemplateTaskTable)

    var title by TemplateTaskTable.title
    var template by TemplateTaskTable.template
    var parent by TemplateTaskTable.parent
    val children: SizedIterable<TemplateTaskEntity> by TemplateTaskEntity optionalReferrersOn TemplateTaskTable.parent
}
