package tech.checkstory.user

import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import tech.checkstory.firebase.user.FirebaseUser
import tech.checkstory.user.UserService.Users.firebaseId

class UserService(private val database: Database) {

    object Users : Table() {
        val firebaseId = varchar("firebaseId", length = 255)
        val name = varchar("name", length = 50)
        val age = integer("age")

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

    suspend fun create(user: User, firebaseUser: FirebaseUser): String {
        return dbQuery {
            Users.insert {
                it[name] = user.name
                it[age] = user.age
                it[firebaseId] = firebaseUser.userId
            }[firebaseId]
        }
    }

    suspend fun read(id: String): User? {
        return dbQuery {
            Users.select { firebaseId eq id }
                .map { User(it[Users.name], it[Users.age]) }
                .singleOrNull()
        }
    }

    suspend fun update(id: String, user: User) {
        dbQuery {
            Users.update({ firebaseId eq id }) {
                it[name] = user.name
                it[age] = user.age
            }
        }
    }

    suspend fun delete(id: String) {
        dbQuery {
            Users.deleteWhere { firebaseId eq id }
        }
    }
}