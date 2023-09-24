package tech.checkstory.template.data

import kotlinx.coroutines.Dispatchers
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import tech.checkstory.auth.user.AuthenticatedScope
import tech.checkstory.domain.command.DtoUUID
import tech.checkstory.template.data.entity.ReminderEntity
import tech.checkstory.template.data.entity.TemplateEntity
import tech.checkstory.template.data.entity.TemplateTaskEntity
import tech.checkstory.template.data.table.ReminderTable
import tech.checkstory.template.data.table.TemplateTable
import tech.checkstory.template.data.table.TemplateTaskTable
import tech.checkstory.template.domain.Template
import tech.checkstory.template.domain.aggregate.Reminder
import tech.checkstory.template.domain.aggregate.TemplateTask
import java.util.*

class RelationalTemplateRepository(private val database: Database) {

    private val logger = LoggerFactory.getLogger(RelationalTemplateRepository::class.java)

    init {
        transaction(database) {
            SchemaUtils.create(TemplateTable)
            SchemaUtils.create(TemplateTaskTable)
            SchemaUtils.create(ReminderTable)
        }
    }

    suspend fun create(template: Template) {
        dbQuery {
            TemplateEntity.new(template.id) {
                userId = template.userId
                title = template.name
                description = template.description
                createdAt = template.createdAt.toLocalDateTime(TimeZone.UTC)
                isDeleted = template.isDeleted
            }
            template.tasks.forEach {
                insertTask(it, null)
            }
            template.reminders.forEach {
                ReminderEntity.fromDomainReminder(it)
            }
        }
    }

    suspend fun updateTitle(templateId: DtoUUID, newTitle: String) {
        dbQuery {
            TemplateEntity.findById(templateId)?.title = newTitle
        }
    }

    suspend fun updateDescription(templateId: DtoUUID, newDescription: String) {
        dbQuery {
            TemplateEntity.findById(templateId)?.description = newDescription
        }
    }

    suspend fun addTask(aggregateId: DtoUUID, taskId: DtoUUID, parentTaskId: DtoUUID?) {
        dbQuery {
            TemplateTaskEntity.new(taskId) {
                title = ""
                parent = parentTaskId?.let { EntityID(it, TemplateTaskTable) }
                template = EntityID(aggregateId, TemplateTable)
            }
        }
    }

    suspend fun renameTask(taskId: DtoUUID, newTitle: String) {
        dbQuery {
            TemplateTaskEntity.findById(taskId)?.title = newTitle
        }
    }

    suspend fun moveTask(taskId: DtoUUID, newParentTaskId: DtoUUID?) {
        dbQuery {
            TemplateTaskEntity.findById(taskId)?.parent = newParentTaskId?.let { EntityID(it, TemplateTaskTable) }
        }
    }

    suspend fun updateLocalPositions(taskIdToLocalPosition: Map<DtoUUID, Long>) {
        dbQuery {
            taskIdToLocalPosition.forEach { (id, position) ->
                TemplateTaskEntity.findById(id)?.sortPosition = position
            }
        }
    }

    suspend fun deleteTask(taskId: DtoUUID) {
        dbQuery {
            TemplateTaskEntity.findById(taskId)?.delete()
        }
    }

    private suspend fun insertTask(templateTask: TemplateTask, parentId: EntityID<UUID>?) {
        val entity = TemplateTaskEntity.new(templateTask.id) {
            title = templateTask.title
            parent = parentId
            template = EntityID(templateTask.templateId, TemplateTable)
            sortPosition = templateTask.sortPosition
        }
        templateTask.children.forEach {
            insertTask(it, entity.id)
        }
    }

    suspend fun addReminder(reminder: Reminder) {
        dbQuery {
            ReminderEntity.fromDomainReminder(reminder)
        }
    }

    suspend fun deleteReminder(reminderId: DtoUUID) {
        dbQuery {
            ReminderEntity.findById(reminderId)?.delete()
        }
    }

    context(AuthenticatedScope)
    suspend fun read(id: UUID): Template? {
        return dbQuery {
            TemplateEntity
                .find {
                    TemplateTable.id eq id and (TemplateTable.userId eq principal.userId)
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
                    TemplateTable.userId eq principal.userId and TemplateTable.isDeleted eq Op.FALSE
                }
                .map(TemplateEntity::toTemplate)
        }
    }

    suspend fun markAsDeleted(id: UUID) {
        dbQuery {
            TemplateEntity.findById(id)?.isDeleted = true
        }
    }

    private suspend fun <T> dbQuery(block: suspend () -> T): T {
        return newSuspendedTransaction(Dispatchers.IO) {
            commonRetry(logger) {
                block()
            }
        }
    }
}
