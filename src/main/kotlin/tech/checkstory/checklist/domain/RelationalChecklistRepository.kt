package tech.checkstory.checklist.domain

import kotlinx.coroutines.Dispatchers
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.kotlin.datetime.datetime
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import tech.checkstory.auth.user.AuthenticatedScope
import tech.checkstory.checklist.domain.aggregate.Checklist
import tech.checkstory.checklist.domain.aggregate.Task
import tech.checkstory.domain.command.DtoUUID
import tech.checkstory.template.data.commonRetry
import tech.checkstory.template.data.entity.TemplateEntity
import tech.checkstory.template.data.table.TemplateTable
import java.util.*

class RelationalChecklistRepository(
    private val database: Database
) : ChecklistRepository {

    init {
        transaction(database) {
            SchemaUtils.create(ChecklistTable)
            SchemaUtils.create(TaskTable)
        }
    }

    context(AuthenticatedScope)
    override suspend fun readAll(): List<Checklist> {
        return dbQuery {
            ChecklistEntity.find {
                ChecklistTable.userId eq principal.userId
            }.map(ChecklistEntity::toChecklist)
        }
    }

    suspend fun create(checklist: Checklist) {
        dbQuery {
            commonRetry {
                ChecklistEntity.new(checklist.id) {
                    userId = checklist.userId
                    templateId = EntityID(checklist.templateId, TemplateTable)
                    notes = checklist.notes
                    createdAt = checklist.createdAt.toLocalDateTime(TimeZone.UTC)
                }
            }
            checklist.tasks.forEach {
                insertTask(it, null)
            }
        }
    }

    private suspend fun insertTask(task: Task, parentId: EntityID<UUID>?) {
        val parent = commonRetry {
            TaskEntity.new(task.id) {
                title = task.title
                checklistId = EntityID(task.checklistId, ChecklistTable)
                parent = parentId
                sortPosition = task.sortPosition
                isChecked = task.isChecked
            }
        }
        task.children.forEach { child ->
            insertTask(child, parent.id)
        }
    }

    context(AuthenticatedScope)
    override suspend fun read(id: UUID): Checklist? {
        return dbQuery {
            ChecklistEntity.find {
                ChecklistTable.id eq id and (ChecklistTable.userId eq principal.userId)
            }
                .map(ChecklistEntity::toChecklist)
                .firstOrNull()
        }
    }

    private suspend fun <T> dbQuery(block: suspend () -> T): T {
        return newSuspendedTransaction(Dispatchers.IO) { block() }
    }

    suspend fun changeTaskChecked(taskId: DtoUUID, isChecked: Boolean) {
        dbQuery {
            TaskEntity.findById(taskId)?.isChecked = isChecked
        }
    }

    suspend fun editNotes(checklistId: DtoUUID, newNotes: String) {
        dbQuery {
            ChecklistEntity.findById(checklistId)?.notes = newNotes
        }
    }

    suspend fun delete(checklistId: DtoUUID) {
        dbQuery {
            ChecklistEntity.findById(checklistId)?.delete()
        }
    }
}

object ChecklistTable : UUIDTable() {

    val template = reference("template_id", TemplateTable, ReferenceOption.CASCADE)
    val userId = varchar("userId", length = 255)
    val notes = varchar("notes", length = 255)
    val createdAt = datetime("createdAt")
}

class ChecklistEntity(id: EntityID<UUID>) : UUIDEntity(id) {

    companion object : UUIDEntityClass<ChecklistEntity>(ChecklistTable)

    var notes by ChecklistTable.notes
    var userId by ChecklistTable.userId
    var createdAt by ChecklistTable.createdAt
    var templateId by ChecklistTable.template
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
        )
    }
}


object TaskTable : UUIDTable() {

    val title = varchar("title", 255)
    val checklist = reference("checklist_id", ChecklistTable, ReferenceOption.CASCADE)
    val parent = optReference("parent_id", TaskTable, ReferenceOption.CASCADE)
    val sortPosition = long("sort_position").default(0)
    val isChecked = bool("is_checked")
}

class TaskEntity(id: EntityID<UUID>) : UUIDEntity(id) {

    companion object : UUIDEntityClass<TaskEntity>(TaskTable)

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
