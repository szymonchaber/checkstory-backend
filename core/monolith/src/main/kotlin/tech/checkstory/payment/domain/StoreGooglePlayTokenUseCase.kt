package tech.checkstory.payment.domain

import tech.checkstory.payment.data.PurchaseRepository
import tech.checkstory.payment.data.google_play.SubscriptionsGoogleApi

class StoreGooglePlayTokenUseCase(
    private val purchaseRepository: PurchaseRepository,
    private val subscriptionsGoogleApi: SubscriptionsGoogleApi
) {

    suspend fun storePaymentToken(userId: String, token: GooglePlayPaymentToken): StoreTokenResult {
        val subscription = subscriptionsGoogleApi.getSubscription(token.purchaseToken)
        return purchaseRepository.storePurchase(userId, token.purchaseToken, subscription.rawValue)
    }
}

sealed interface StoreTokenResult {

    data object Success : StoreTokenResult
    data object TokenAssignedToAnotherUser : StoreTokenResult
}
