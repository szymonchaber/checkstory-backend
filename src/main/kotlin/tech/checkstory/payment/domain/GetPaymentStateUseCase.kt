package tech.checkstory.payment.domain

import kotlinx.datetime.Clock
import tech.checkstory.payment.data.GiftedSubscriptionRepository
import tech.checkstory.payment.data.PurchaseRepository

class GetPaymentStateUseCase(
    private val purchaseRepository: PurchaseRepository,
    private val giftedSubscriptionRepository: GiftedSubscriptionRepository,
) {

    suspend fun getPaymentStateForUser(userId: String): PaymentState {
        val isAnySubscriptionActive = purchaseRepository.getByUserId(userId)
            .any {
                Clock.System.now() < it.expirationDate
            }
        val wasGiftedSubscription = giftedSubscriptionRepository.hasSubscriptionGift(userId)
        return PaymentState(isSubscriptionActive = isAnySubscriptionActive || wasGiftedSubscription)
    }
}
