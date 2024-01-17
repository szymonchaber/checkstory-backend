package tech.checkstory.payment.domain

import kotlinx.datetime.Clock
import tech.checkstory.payment.data.GiftedSubscriptionRepository
import tech.checkstory.payment.data.PurchaseRepository

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

        if (purchases.none { Clock.System.now() < it.expirationDate }) {
            purchases.forEach {
                storeGooglePlayTokenUseCase.storePaymentToken(userId, it.googlePaymentToken)
            }
            return purchaseRepository.getByUserId(userId).any { Clock.System.now() < it.expirationDate }
        }
        return true
    }
}
