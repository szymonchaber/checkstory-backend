package tech.checkstory.payment.domain

sealed interface StoreTokenResult {

    data object Success : StoreTokenResult
    data object TokenAssignedToAnotherUser : StoreTokenResult
}
