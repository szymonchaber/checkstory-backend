package tech.checkstory

import com.google.firebase.auth.FirebaseAuth
import tech.checkstory.checklist.data.ChecklistRepository
import tech.checkstory.core.domain.auth.AuthenticatedScope
import tech.checkstory.core.domain.auth.DeleteUserUseCase
import tech.checkstory.core.domain.auth.UserService
import tech.checkstory.notification.data.FirebaseMessagingTokenRepository
import tech.checkstory.payment.domain.GiftedSubscriptionRepository
import tech.checkstory.payment.domain.PurchaseRepository
import tech.checkstory.template.data.TemplateRepository

class DeleteUserUseCaseImpl(
    private val userService: UserService,
    private val purchaseRepository: PurchaseRepository,
    private val giftedSubscriptionRepository: GiftedSubscriptionRepository,
    private val firebaseMessagingTokenRepository: FirebaseMessagingTokenRepository,
    private val templateRepository: TemplateRepository,
    private val checklistRepository: ChecklistRepository,
) : DeleteUserUseCase {

    context(AuthenticatedScope)
    override suspend fun deleteUser() {
        purchaseRepository.deleteUserPurchases()
        giftedSubscriptionRepository.deleteUserGifts()
        firebaseMessagingTokenRepository.deleteUserFirebaseTokens()
        checklistRepository.deleteUserChecklists()
        templateRepository.deleteUserTemplates()
        userService.delete()
        FirebaseAuth.getInstance().deleteUser(principal.userId)
    }
}
