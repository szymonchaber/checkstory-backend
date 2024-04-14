package tech.checkstory.payment.domain

import com.google.api.services.androidpublisher.model.SubscriptionPurchaseV2
import kotlinx.coroutines.Dispatchers
import kotlinx.datetime.Instant
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import tech.checkstory.core.domain.auth.AuthenticatedScope

class PurchaseRepository(database: Database) {

    private object GooglePlayBillingPurchases : Table("google_play_billing_purchase") {

        val token = varchar("google_play_token", length = 255)
        val userId = varchar("user_id", length = 255)
        val expirationDate = long("expiration_date")

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
                it[GooglePlayBillingPurchases.expirationDate] =
                    Instant.parse(rawValue.lineItems.first().expiryTime).epochSeconds
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

    suspend fun storePurchase(
        userId: String,
        purchaseToken: String,
        rawValue: SubscriptionPurchaseV2
    ): StoreTokenResult {
        return dbQuery {
            val existing =
                GooglePlayBillingPurchases
                    .select { GooglePlayBillingPurchases.token eq purchaseToken }
                    .firstOrNull()
            if (existing == null) {
                GooglePlayBillingPurchases.insert {
                    it[GooglePlayBillingPurchases.token] = purchaseToken
                    it[GooglePlayBillingPurchases.expirationDate] =
                        Instant.parse(rawValue.lineItems.first().expiryTime).epochSeconds
                    it[GooglePlayBillingPurchases.userId] = userId
                }
                StoreTokenResult.Success
            } else {
                val currentTokenUser = existing[GooglePlayBillingPurchases.userId]
                if (currentTokenUser == userId) {
                    GooglePlayBillingPurchases.update(where = {
                        (GooglePlayBillingPurchases.token eq purchaseToken) and (GooglePlayBillingPurchases.userId eq userId)
                    }) {
                        it[GooglePlayBillingPurchases.token] = purchaseToken
                        it[GooglePlayBillingPurchases.expirationDate] =
                            Instant.parse(rawValue.lineItems.first().expiryTime).epochSeconds
                    }
                    StoreTokenResult.Success
                } else {
                    StoreTokenResult.TokenAssignedToAnotherUser
                }
            }
        }
    }

    suspend fun getByUserId(userId: String): List<Purchase> {
        return dbQuery {
            GooglePlayBillingPurchases.select { GooglePlayBillingPurchases.userId eq userId }
                .map {
                    Purchase(
                        Instant.fromEpochSeconds(it[GooglePlayBillingPurchases.expirationDate]),
                        GooglePlayPaymentToken(it[GooglePlayBillingPurchases.token])
                    )
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

    context(AuthenticatedScope)
    suspend fun deleteUserPurchases() {
        dbQuery {
            GooglePlayBillingPurchases.deleteWhere { GooglePlayBillingPurchases.userId eq principal.userId }
        }
    }
}
