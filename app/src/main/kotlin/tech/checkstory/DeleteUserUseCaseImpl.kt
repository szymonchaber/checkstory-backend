package tech.checkstory

import tech.checkstory.checklist.domain.ChecklistRepository
import tech.checkstory.core.domain.auth.AuthenticatedScope
import tech.checkstory.core.domain.auth.DeleteUserUseCase
import tech.checkstory.core.domain.auth.UserService
import tech.checkstory.notification.data.FirebaseMessagingTokenRepository
import tech.checkstory.payment.data.GiftedSubscriptionRepository
import tech.checkstory.payment.data.PurchaseRepository
import tech.checkstory.template.data.RelationalTemplateRepository

class DeleteUserUseCaseImpl(
    private val userService: UserService,
    private val purchaseRepository: PurchaseRepository,
    private val giftedSubscriptionRepository: GiftedSubscriptionRepository,
    private val firebaseMessagingTokenRepository: FirebaseMessagingTokenRepository,
    private val templateRepository: RelationalTemplateRepository,
    private val checklistRepository: ChecklistRepository
) : DeleteUserUseCase {

    context(AuthenticatedScope)
    override suspend fun deleteUser() {
        purchaseRepository.deleteUserPurchases()
        giftedSubscriptionRepository.deleteUserGifts()
        firebaseMessagingTokenRepository.deleteUserFirebaseTokens()
        // remove stuff from EventStoreDb here
        checklistRepository.deleteUserChecklists()
        templateRepository.deleteUserTemplates()
        userService.delete()
        // remove the account from Firebase here
    }
}
