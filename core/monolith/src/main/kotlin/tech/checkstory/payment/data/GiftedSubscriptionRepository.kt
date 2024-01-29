package tech.checkstory.payment.data

import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import tech.checkstory.core.domain.auth.AuthenticatedScope

class GiftedSubscriptionRepository(database: Database) {

    private object GiftedSubscription : Table("gifted_subscription") {

        val userId = varchar("user_id", length = 255)

        @Suppress("unused")
        val reason = varchar("reason", length = 255)

        override val primaryKey = PrimaryKey(userId)
    }

    init {
        transaction(database) {
            SchemaUtils.create(GiftedSubscription)
        }
    }

    context (AuthenticatedScope)
    suspend fun read(): Boolean {
        return hasSubscriptionGift(principal.userId)
    }

    suspend fun hasSubscriptionGift(userId: String): Boolean {
        return dbQuery {
            GiftedSubscription
                .select { GiftedSubscription.userId eq userId }
                .firstOrNull() != null
        }
    }

    context(AuthenticatedScope)
    suspend fun deleteByUserId() {
        dbQuery {
            GiftedSubscription.deleteWhere { userId eq principal.userId }
        }
    }

    private suspend fun <T> dbQuery(block: suspend () -> T): T {
        return newSuspendedTransaction(Dispatchers.IO) { block() }
    }
}
