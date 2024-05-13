package tech.checkstory.auth.data

import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import tech.checkstory.auth.data.UserServiceImpl.UserTable.email
import tech.checkstory.auth.data.UserServiceImpl.UserTable.firebaseId
import tech.checkstory.core.domain.auth.AuthenticatedScope
import tech.checkstory.core.domain.auth.User
import tech.checkstory.core.domain.auth.UserService

class UserServiceImpl(private val database: Database) : UserService {

    private object UserTable : Table("user") {
        val firebaseId = varchar("firebase_id", length = 255)
        val email = varchar("email", length = 254).nullable().uniqueIndex()
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

    override suspend fun create(userId: String, email: String?): String {
        return dbQuery {
            UserTable.insert {
                it[firebaseId] = userId
                it[UserTable.email] = email
            }[firebaseId]
        }
    }

    context(AuthenticatedScope)
    suspend fun read(): User? {
        return readById(principal.userId)
    }

    override suspend fun readById(userId: String): User? {
        return dbQuery {
            UserTable.select { firebaseId eq userId }
                .map {
                    User(
                        userId = it[firebaseId],
                        email = it[email]
                    )
                }
                .singleOrNull()
        }
    }

    context(AuthenticatedScope)
    override suspend fun delete() {
        dbQuery {
            UserTable.deleteWhere { firebaseId eq principal.userId }
        }
    }
}
