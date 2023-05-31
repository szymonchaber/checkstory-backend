package tech.checkstory.payment.domain

import kotlinx.datetime.Clock
import tech.checkstory.payment.data.PurchaseRepository

class GetPaymentStateUseCase(
    private val purchaseRepository: PurchaseRepository
) {

    suspend fun getPaymentStateForUser(userId: String): PaymentState {
        val isAnySubscriptionActive = purchaseRepository.getByUserId(userId)
            .any {
                Clock.System.now() < it.expirationDate
            }
        return PaymentState(isSubscriptionActive = isAnySubscriptionActive)
    }
}
