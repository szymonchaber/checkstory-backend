package tech.checkstory.payment.data

import com.google.api.services.androidpublisher.model.SubscriptionPurchaseV2
import kotlinx.coroutines.Dispatchers
import kotlinx.datetime.Instant
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import tech.checkstory.auth.user.AuthenticatedScope
import tech.checkstory.payment.domain.StoreTokenResult

class PurchaseRepository(database: Database) {

    private object GooglePlayBillingPurchases : Table() {

        val token = varchar("googlePlayToken", length = 255)
        val userId = varchar("userId", length = 255)
        val expirationDate = long("expirationDate")

        override val primaryKey = PrimaryKey(token)
    }

    init {
        transaction(database) {
            SchemaUtils.create(GooglePlayBillingPurchases)
        }
    }

    suspend fun updatePurchase(purchaseToken: String, rawValue: SubscriptionPurchaseV2) {
        dbQuery {
            GooglePlayBillingPurchases.update(where = {
                GooglePlayBillingPurchases.token eq purchaseToken
            }) {
                it[expirationDate] = Instant.parse(rawValue.lineItems.first().expiryTime).epochSeconds
            }
        }
    }

    suspend fun containsToken(purchaseToken: String) {
        dbQuery {
            GooglePlayBillingPurchases.select {
                GooglePlayBillingPurchases.token eq purchaseToken
            }
        }
    }

    context(AuthenticatedScope)
    suspend fun storePurchase(purchaseToken: String, rawValue: SubscriptionPurchaseV2): StoreTokenResult {
        return dbQuery {
            val existing =
                GooglePlayBillingPurchases
                    .select { GooglePlayBillingPurchases.token eq purchaseToken }
                    .firstOrNull()
            if (existing == null) {
                GooglePlayBillingPurchases.insert {
                    it[token] = purchaseToken
                    it[expirationDate] = Instant.parse(rawValue.lineItems.first().expiryTime).epochSeconds
                    it[userId] = principal.userId
                }
                StoreTokenResult.Success
            } else {
                val currentTokenUser = existing[GooglePlayBillingPurchases.userId]
                if (currentTokenUser == principal.userId) {
                    GooglePlayBillingPurchases.update {
                        it[token] = purchaseToken
                        it[expirationDate] = Instant.parse(rawValue.lineItems.first().expiryTime).epochSeconds
                    }
                    StoreTokenResult.Success
                } else {
                    StoreTokenResult.TokenAssignedToAnotherUser
                }
            }
        }
    }

    context (AuthenticatedScope)
    suspend fun read(): List<Purchase> {
        return getByUserId(principal.userId)
    }

    suspend fun getByUserId(userId: String): List<Purchase> {
        return dbQuery {
            GooglePlayBillingPurchases.select { GooglePlayBillingPurchases.userId eq userId }
                .map {
                    Purchase(Instant.fromEpochSeconds(it[GooglePlayBillingPurchases.expirationDate]))
                }
        }
    }

    suspend fun getUserIdWithToken(purchaseToken: String): String? {
        return dbQuery {
            GooglePlayBillingPurchases.select {
                GooglePlayBillingPurchases.token eq purchaseToken
            }.map {
                it[GooglePlayBillingPurchases.userId]
            }
                .firstOrNull()
        }
    }

    private suspend fun <T> dbQuery(block: suspend () -> T): T {
        return newSuspendedTransaction(Dispatchers.IO) { block() }
    }
}

data class Purchase(val expirationDate: Instant)
