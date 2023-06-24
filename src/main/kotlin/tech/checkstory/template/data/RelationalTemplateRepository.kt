package tech.checkstory.template.data

import kotlinx.coroutines.Dispatchers
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import tech.checkstory.auth.user.AuthenticatedScope
import tech.checkstory.template.data.entity.TemplateEntity
import tech.checkstory.template.data.entity.TemplateTaskEntity
import tech.checkstory.template.data.table.TemplateTaskTable
import tech.checkstory.template.data.table.Templates
import tech.checkstory.template.domain.Template
import tech.checkstory.template.domain.aggregate.TemplateTask
import java.util.*

class RelationalTemplateRepository(private val database: Database) {

    init {
        transaction(database) {
            SchemaUtils.create(Templates)
            SchemaUtils.create(TemplateTaskTable)
        }
    }

    suspend fun create(template: Template) {
        dbQuery {
            TemplateEntity.new(template.id) {
                userId = template.userId
                name = template.name
                description = template.description
                createdAt = template.createdAt.toLocalDateTime(TimeZone.UTC)
            }
            template.tasks.forEach {
                insertTask(it, null)
            }
        }
    }

    private fun insertTask(templateTask: TemplateTask, parentId: EntityID<UUID>?) {
        val entity = TemplateTaskEntity.new(templateTask.id) {
            title = templateTask.title
            parent = parentId
            template = EntityID(templateTask.templateId, Templates)
        }
        templateTask.children.forEach {
            insertTask(it, entity.id)
        }
    }

    context(AuthenticatedScope)
    suspend fun read(id: UUID): Template? {
        return dbQuery {
            TemplateEntity
                .find {
                    Templates.id eq id and (Templates.userId eq principal.userId)
                }
                .singleOrNull()
                ?.toTemplate()
        }
    }

    context(AuthenticatedScope)
    suspend fun readAll(): List<Template> {
        return dbQuery {
            TemplateEntity
                .find {
                    Templates.userId eq principal.userId
                }
                .map(TemplateEntity::toTemplate)
        }
    }

    context(AuthenticatedScope)
    suspend fun readTasks(): List<TemplateTask> {
        return dbQuery {
            TemplateTaskEntity.find {
                TemplateTaskTable.parent eq null
            }
                .map(TemplateTaskEntity::toTemplateTask)
        }
    }

    suspend fun delete(id: UUID) {
        dbQuery {
            TemplateEntity.findById(id)?.delete()
        }
    }

    private suspend fun <T> dbQuery(block: suspend () -> T): T {
        return newSuspendedTransaction(Dispatchers.IO) { block() }
    }
}
