package tech.checkstory.checklist.domain

import kotlinx.coroutines.Dispatchers
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.kotlin.datetime.datetime
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import tech.checkstory.checklist.domain.aggregate.Checklist
import tech.checkstory.checklist.domain.aggregate.ChecklistState
import tech.checkstory.checklist.domain.aggregate.Task
import tech.checkstory.core.domain.auth.AuthenticatedScope
import tech.checkstory.data.UpsertableUUIDEntityClass
import tech.checkstory.template.data.commonRetry
import tech.checkstory.template.data.entity.TemplateEntity
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
                        checklist eq checklistEntity.id
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

object ChecklistTable : UUIDTable() {

    val template = reference("template_id", TemplateTable, ReferenceOption.CASCADE)
    val userId = varchar("user_id", length = 255)
    val notes = varchar("notes", length = 255)
    val createdAt = datetime("created_at")
    val isDeleted = bool("is_deleted")
}

class ChecklistEntity(id: EntityID<UUID>) : UUIDEntity(id) {

    companion object : UpsertableUUIDEntityClass<ChecklistEntity>(ChecklistTable)

    var notes by ChecklistTable.notes
    var userId by ChecklistTable.userId
    var createdAt by ChecklistTable.createdAt
    var templateId by ChecklistTable.template
    var isDeleted by ChecklistTable.isDeleted
    val tasks: SizedIterable<TaskEntity> by TaskEntity referrersOn TaskTable.checklist
    val template by TemplateEntity referencedOn ChecklistTable.template

    fun toChecklist(): Checklist {
        return Checklist(
            id = id.value,
            userId = userId,
            title = template.title,
            description = template.description,
            createdAt = createdAt.toInstant(TimeZone.UTC),
            notes = notes,
            templateId = templateId.value,
            tasks = tasks
                .orderBy(TaskTable.sortPosition to SortOrder.ASC)
                .filter {
                    it.parent == null
                }
                .map(TaskEntity::toTask),
            isDeleted = isDeleted
        )
    }
}


object TaskTable : UUIDTable("task") {

    val title = varchar("title", 255)
    val checklist = reference("checklist_id", ChecklistTable, ReferenceOption.CASCADE)
    val parent = optReference("parent_id", TaskTable, ReferenceOption.CASCADE)
    val sortPosition = long("sort_position").default(0)
    val isChecked = bool("is_checked")
}

class TaskEntity(id: EntityID<UUID>) : UUIDEntity(id) {

    companion object : UpsertableUUIDEntityClass<TaskEntity>(TaskTable)

    var title by TaskTable.title
    var checklistId by TaskTable.checklist
    var parent by TaskTable.parent
    var sortPosition by TaskTable.sortPosition
    val children: SizedIterable<TaskEntity> by TaskEntity optionalReferrersOn TaskTable.parent

    var isChecked by TaskTable.isChecked

    fun toTask(): Task {
        return Task(
            id.value,
            checklistId.value,
            title,
            sortPosition,
            isChecked,
            children
                .orderBy(TaskTable.sortPosition to SortOrder.ASC)
                .map(TaskEntity::toTask)
        )
    }
}
