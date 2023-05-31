package tech.checkstory.notification.data

import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import tech.checkstory.auth.user.AuthenticatedScope
import tech.checkstory.notification.domain.FirebaseMessagingToken

class FirebaseMessagingTokenRepository(private val database: Database) {

    private object FirebaseMessagingTokenToUser : Table() {
        val token = varchar("firebaseMessagingToken", length = 255)
        val userId = varchar("userId", length = 255)

        override val primaryKey = PrimaryKey(token)
    }

    init {
        transaction(database) {
            SchemaUtils.create(FirebaseMessagingTokenToUser)
        }
    }

    context(AuthenticatedScope)
    suspend fun storeToken(token: FirebaseMessagingToken) {
        dbQuery {
            FirebaseMessagingTokenToUser.insertIgnore {
                it[this.token] = token.token
                it[userId] = principal.userId
            }
        }
    }

    context (AuthenticatedScope)
    suspend fun readAll(): List<FirebaseMessagingToken> {
        return getByUserId(principal.userId)
    }

    suspend fun readAllById(userId: String): List<FirebaseMessagingToken> {
        return getByUserId(userId)
    }

    private suspend fun getByUserId(userId: String): List<FirebaseMessagingToken> {
        return dbQuery {
            FirebaseMessagingTokenToUser.select { FirebaseMessagingTokenToUser.userId eq userId }
                .map {
                    FirebaseMessagingToken(it[FirebaseMessagingTokenToUser.token])
                }
        }
    }

    suspend fun remove(token: FirebaseMessagingToken) {
        dbQuery {
            FirebaseMessagingTokenToUser.deleteWhere { FirebaseMessagingTokenToUser.token eq token.token }
        }
    }

    private suspend fun <T> dbQuery(block: suspend () -> T): T {
        return newSuspendedTransaction(Dispatchers.IO) { block() }
    }
}