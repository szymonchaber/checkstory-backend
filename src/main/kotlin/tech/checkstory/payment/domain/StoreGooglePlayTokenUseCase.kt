package tech.checkstory.payment.domain

import tech.checkstory.auth.user.AuthenticatedScope
import tech.checkstory.payment.data.PurchaseRepository
import tech.checkstory.payment.data.google_play.SubscriptionsGoogleApi

class StoreGooglePlayTokenUseCase(
    private val purchaseRepository: PurchaseRepository,
    private val subscriptionsGoogleApi: SubscriptionsGoogleApi
) {

    context(AuthenticatedScope)
    suspend fun storePaymentToken(token: GooglePlayPaymentToken): StoreTokenResult {
        val subscription = subscriptionsGoogleApi.getSubscription(token.purchaseToken)
        return purchaseRepository.storePurchase(token.purchaseToken, subscription.rawValue)
    }
}

sealed interface StoreTokenResult {

    data object Success : StoreTokenResult
    data object TokenAssignedToAnotherUser : StoreTokenResult
}