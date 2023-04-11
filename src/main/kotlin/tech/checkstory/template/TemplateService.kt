package tech.checkstory.template

import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import tech.checkstory.firebase.user.FirebaseUser
import tech.checkstory.template.TemplateService.Templates.id

class TemplateService(private val database: Database) {

    object Templates : Table() {
        val id = integer("id").autoIncrement()
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

    suspend fun create(template: Template, firebaseUser: FirebaseUser): Int {
        return dbQuery {
            Templates.insert {
                it[userId] = firebaseUser.userId
                it[name] = template.name
                it[description] = template.description
            }[id]
        }
    }

    suspend fun create(newId: Int, template: Template, firebaseUser: FirebaseUser) {
        dbQuery {
            Templates.insert {
                it[id] = newId
                it[userId] = firebaseUser.userId
                it[name] = template.name
                it[description] = template.description
            }
        }
    }

    suspend fun read(firebaseUser: FirebaseUser, id: Int): Template? {
        return dbQuery {
            Templates.select { Templates.id eq id and (Templates.userId eq firebaseUser.userId) }
                .map {
                    Template(
                        name = it[Templates.name],
                        description = it[Templates.description]
                    )
                }
                .singleOrNull()
        }
    }

    suspend fun read(firebaseUser: FirebaseUser): List<Template> {
        return dbQuery {
            Templates.select { Templates.userId eq firebaseUser.userId }
                .map {
                    Template(
                        name = it[Templates.name],
                        description = it[Templates.description]
                    )
                }
        }
    }

    suspend fun updateTitle(firebaseUser: FirebaseUser, templateId: Int, newTitle: String) {
        dbQuery {
            Templates.update({ (id eq templateId) and (Templates.userId eq firebaseUser.userId) }) {
                it[name] = newTitle
            }
        }
    }

    suspend fun delete(firebaseUser: FirebaseUser, id: Int) {
        dbQuery {
            Templates.deleteWhere { Templates.id eq id and (userId eq firebaseUser.userId) }
        }
    }
}

@Serializable
data class Template(val name: String, val description: String)
