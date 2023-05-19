package tech.checkstory.payment.domain

import tech.checkstory.payment.data.PaymentRepository
import tech.checkstory.payment.data.google_play.SubscriptionsGoogleApi
import tech.checkstory.payment.domain.model.SubscriptionPurchaseWrapper.SubscriptionState.Companion.shouldGrantAccess

class GetPaymentStateUseCase(
    private val repository: PaymentRepository,
    private val subscriptionsGoogleApi: SubscriptionsGoogleApi
) {

    suspend fun getPaymentStateForUser(userId: String): PaymentState {
        val isAnySubscriptionActive = repository.getByUserId(userId)
            .any {
                subscriptionsGoogleApi.getSubscription(it.purchaseToken).subscriptionState.shouldGrantAccess
            }
        return PaymentState(isSubscriptionActive = isAnySubscriptionActive)
    }
}
