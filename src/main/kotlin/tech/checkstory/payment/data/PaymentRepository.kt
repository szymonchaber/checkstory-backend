package tech.checkstory.payment.data

import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import tech.checkstory.auth.user.AuthenticatedScope
import tech.checkstory.payment.domain.GooglePlayPaymentToken

class PaymentRepository(private val database: Database) {

    object GooglePlayTokenToUser : Table() {
        val token = varchar("googlePlayToken", length = 255)
        val userId = varchar("userId", length = 255)

        override val primaryKey = PrimaryKey(token)
    }

    init {
        transaction(database) {
            SchemaUtils.create(GooglePlayTokenToUser)
        }
    }

    suspend fun <T> dbQuery(block: suspend () -> T): T {
        return newSuspendedTransaction(Dispatchers.IO) { block() }
    }

    context(AuthenticatedScope)
    suspend fun storeToken(googlePlayToken: GooglePlayPaymentToken) {
        dbQuery {
            GooglePlayTokenToUser.insert {
                it[token] = googlePlayToken.purchaseToken
                it[userId] = principal.userId
            }
        }
    }

    context (AuthenticatedScope)
    suspend fun read(): List<GooglePlayPaymentToken> {
        return getByUserId(principal.userId)
    }

    suspend fun getByUserId(userId: String): List<GooglePlayPaymentToken> {
        return dbQuery {
            GooglePlayTokenToUser.select { GooglePlayTokenToUser.userId eq userId }
                .map {
                    GooglePlayPaymentToken(it[GooglePlayTokenToUser.token])
                }
        }
    }
}