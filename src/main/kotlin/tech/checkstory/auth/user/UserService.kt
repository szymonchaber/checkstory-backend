package tech.checkstory.auth.user

import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import tech.checkstory.auth.user.UserService.Users.firebaseId

class UserService(private val database: Database) {

    object Users : Table() {
        val firebaseId = varchar("firebaseId", length = 255)
        override val primaryKey = PrimaryKey(firebaseId)
    }

    init {
        transaction(database) {
            SchemaUtils.create(Users)
        }
    }

    suspend fun <T> dbQuery(block: suspend () -> T): T {
        return newSuspendedTransaction(Dispatchers.IO) { block() }
    }

    suspend fun create(user: CreateUserPayload, userID: String): String {
        return dbQuery {
            Users.insert {
                it[firebaseId] = userID
            }[firebaseId]
        }
    }

    suspend fun read(id: String): User? {
        return dbQuery {
            Users.select { firebaseId eq id }
                .map { User(it[Users.firebaseId]) }
                .singleOrNull()
        }
    }

    suspend fun update(id: String, user: User) {
        dbQuery {
            Users.update({ firebaseId eq id }) {
//                it[name] = user.name
            }
        }
    }

    suspend fun delete(id: String) {
        dbQuery {
            Users.deleteWhere { firebaseId eq id }
        }
    }
}