package tech.checkstory.payment.data

import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import tech.checkstory.auth.user.AuthenticatedScope

class GiftedSubscriptionRepository(database: Database) {

    private object GiftedSubscriptions : Table() {

        val userId = varchar("userId", length = 255)

        @Suppress("unused")
        val reason = varchar("reason", length = 255)

        override val primaryKey = PrimaryKey(userId)
    }

    init {
        transaction(database) {
            SchemaUtils.create(GiftedSubscriptions)
        }
    }

    context (AuthenticatedScope)
    suspend fun read(): Boolean {
        return hasSubscriptionGift(principal.userId)
    }

    suspend fun hasSubscriptionGift(userId: String): Boolean {
        return dbQuery {
            GiftedSubscriptions
                .select { GiftedSubscriptions.userId eq userId }
                .firstOrNull() != null
        }
    }

    private suspend fun <T> dbQuery(block: suspend () -> T): T {
        return newSuspendedTransaction(Dispatchers.IO) { block() }
    }
}
