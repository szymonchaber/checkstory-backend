package tech.checkstory.payment.domain

import tech.checkstory.auth.user.AuthenticatedScope
import tech.checkstory.payment.data.PaymentRepository

class StoreGooglePlayTokenUseCase(private val repository: PaymentRepository) {

    context(AuthenticatedScope)
    suspend fun storePaymentToken(token: GooglePlayPaymentToken): StoreTokenResult {
        return repository.storeToken(token)
    }
}

sealed interface StoreTokenResult {

    data object Success : StoreTokenResult
    data object TokenAssignedToAnotherUser : StoreTokenResult
}