package tech.checkstory.auth.user

import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import tech.checkstory.auth.user.UserService.UserTable.firebaseId

class UserService(private val database: Database) {

    private object UserTable : Table("user") {
        val firebaseId = varchar("firebase_id", length = 255)
        override val primaryKey = PrimaryKey(firebaseId)
    }

    init {
        transaction(database) {
            SchemaUtils.create(UserTable)
        }
    }

    suspend fun <T> dbQuery(block: suspend () -> T): T {
        return newSuspendedTransaction(Dispatchers.IO) { block() }
    }

    suspend fun create(user: CreateUserPayload, userID: String): String {
        return dbQuery {
            UserTable.insert {
                it[firebaseId] = userID
            }[firebaseId]
        }
    }

    context(AuthenticatedScope)
    suspend fun read(): User? {
        return readById(principal.userId)
    }

    suspend fun readById(userId: String): User? {
        return dbQuery {
            UserTable.select { firebaseId eq userId }
                .map {
                    User(
                        userId = it[firebaseId],
                    )
                }
                .singleOrNull()
        }
    }

    context(AuthenticatedScope)
    suspend fun delete() {
        dbQuery {
            UserTable.deleteWhere { firebaseId eq principal.userId }
        }
    }
}
