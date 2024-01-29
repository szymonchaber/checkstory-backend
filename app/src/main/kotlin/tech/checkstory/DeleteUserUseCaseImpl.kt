package tech.checkstory

import tech.checkstory.core.domain.auth.AuthenticatedScope
import tech.checkstory.core.domain.auth.DeleteUserUseCase
import tech.checkstory.core.domain.auth.UserService
import tech.checkstory.payment.data.GiftedSubscriptionRepository
import tech.checkstory.payment.data.PurchaseRepository

class DeleteUserUseCaseImpl(
    private val userService: UserService,
    private val purchaseRepository: PurchaseRepository,
    private val giftedSubscriptionRepository: GiftedSubscriptionRepository
) : DeleteUserUseCase {

    context(AuthenticatedScope)
    override suspend fun deleteUser() {
        purchaseRepository.deleteUserPurchases()
        giftedSubscriptionRepository.deleteByUserId()
        userService.delete()
    }
}
