package tech.checkstory.payment.domain

import kotlinx.datetime.Clock

class GetPaymentStateUseCase(
    private val purchaseRepository: PurchaseRepository,
    private val giftedSubscriptionRepository: GiftedSubscriptionRepository,
    private val storeGooglePlayTokenUseCase: StoreGooglePlayTokenUseCase
) {

    suspend fun getPaymentStateForUser(userId: String): PaymentState {
        return PaymentState(
            isSubscriptionActive = giftedSubscriptionRepository.hasSubscriptionGift(userId)
                    || hasActiveSubscription(userId)
        )
    }

    private suspend fun hasActiveSubscription(userId: String): Boolean {
        val purchases = purchaseRepository.getByUserId(userId)
        if (purchases.isEmpty()) {
            return false
        }

        if (purchases.any(::isActive)) {
            return true
        }
        purchases.forEach {
            storeGooglePlayTokenUseCase.refreshSubscriptionStatus(userId, it.googlePaymentToken)
        }
        return purchaseRepository.getByUserId(userId).any(::isActive)
    }

    private fun isActive(purchase: Purchase): Boolean {
        return Clock.System.now() < purchase.expirationDate
    }
}
