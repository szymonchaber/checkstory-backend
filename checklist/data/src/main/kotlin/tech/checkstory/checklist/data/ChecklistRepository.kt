package tech.checkstory.checklist.data

import kotlinx.coroutines.Dispatchers
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import tech.checkstory.checklist.domain.aggregate.Checklist
import tech.checkstory.checklist.domain.aggregate.ChecklistState
import tech.checkstory.checklist.domain.aggregate.Task
import tech.checkstory.core.domain.auth.AuthenticatedScope
import tech.checkstory.data.commonRetry
import tech.checkstory.template.data.table.TemplateTable
import java.util.*

class ChecklistRepository(
    private val database: Database
) {

    private val logger = LoggerFactory.getLogger(ChecklistRepository::class.java)

    init {
        transaction(database) {
            SchemaUtils.create(ChecklistTable)
            SchemaUtils.create(TaskTable)
        }
    }

    context(AuthenticatedScope)
    suspend fun readAllNotDeleted(): List<Checklist> {
        return dbQuery {
            ChecklistEntity.find {
                ChecklistTable.userId eq principal.userId and (ChecklistTable.isDeleted eq Op.FALSE)
            }
                .filterNot {
                    it.template.isDeleted // TODO can we filter on some kind of join above?
                }
                .map(ChecklistEntity::toChecklist)
        }
    }

    suspend fun upsert(existingChecklist: ChecklistState.Existing) {
        dbQuery {
            ChecklistEntity.new(existingChecklist.id) {
                userId = existingChecklist.userId
                templateId = EntityID(existingChecklist.templateId, TemplateTable)
                notes = existingChecklist.notes
                createdAt = existingChecklist.createdAt.toLocalDateTime(TimeZone.UTC)
                isDeleted = existingChecklist.isDeleted
            }
            existingChecklist.tasks.forEach {
                insertTask(it, null)
            }
        }
    }

    private fun insertTask(task: Task, parentId: EntityID<UUID>?) {
        val parent = TaskEntity.new(task.id) {
            title = task.title
            checklistId = EntityID(task.checklistId, ChecklistTable)
            parent = parentId
            sortPosition = task.sortPosition
            isChecked = task.isChecked
        }
        task.children.forEach { child ->
            insertTask(child, parent.id)
        }
    }

    context(AuthenticatedScope)
    suspend fun read(id: UUID): Checklist? {
        return dbQuery {
            ChecklistEntity.find {
                ChecklistTable.id eq id and (ChecklistTable.userId eq principal.userId) and (ChecklistTable.isDeleted eq Op.FALSE)
            }
                .filterNot {
                    it.template.isDeleted // TODO can we filter on some kind of join above?
                }
                .map(ChecklistEntity::toChecklist)
                .firstOrNull()
        }
    }

    context(AuthenticatedScope)
    suspend fun deleteUserChecklists() {
        dbQuery {
            ChecklistEntity.find { ChecklistTable.userId eq principal.userId }
                .forEach { checklistEntity ->
                    TaskTable.deleteWhere {
                        TaskTable.checklist eq checklistEntity.id
                    }
                    checklistEntity.delete()
                }
        }
    }

    private suspend fun <T> dbQuery(tag: Any? = null, block: suspend () -> T): T {
        return commonRetry(logger, tag, maxRetries = 3) {
            newSuspendedTransaction(Dispatchers.IO) {
                block()
            }
        }
    }
}
