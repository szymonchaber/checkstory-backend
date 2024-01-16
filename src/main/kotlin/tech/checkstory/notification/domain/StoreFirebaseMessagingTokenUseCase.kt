package tech.checkstory.notification.domain

import tech.checkstory.core.domain.auth.AuthenticatedScope
import tech.checkstory.notification.data.FirebaseMessagingTokenRepository

class StoreFirebaseMessagingTokenUseCase(private val repository: FirebaseMessagingTokenRepository) {

    context(AuthenticatedScope)
    suspend fun storeFirebaseMessagingToken(token: FirebaseMessagingToken) {
        repository.storeToken(token)
    }
}
