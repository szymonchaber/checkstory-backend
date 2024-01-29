package tech.checkstory

import com.google.firebase.auth.FirebaseAuth
import tech.checkstory.checklist.domain.ChecklistRepository
import tech.checkstory.checklist.domain.aggregate.Checklist
import tech.checkstory.checklist.domain.event.ChecklistEvent
import tech.checkstory.core.domain.auth.AuthenticatedScope
import tech.checkstory.core.domain.auth.DeleteUserUseCase
import tech.checkstory.core.domain.auth.UserService
import tech.checkstory.data.EventRepository
import tech.checkstory.notification.data.FirebaseMessagingTokenRepository
import tech.checkstory.payment.data.GiftedSubscriptionRepository
import tech.checkstory.payment.data.PurchaseRepository
import tech.checkstory.template.data.RelationalTemplateRepository
import tech.checkstory.template.domain.aggregate.Template
import tech.checkstory.template.domain.event.TemplateEvent

class DeleteUserUseCaseImpl(
    private val userService: UserService,
    private val purchaseRepository: PurchaseRepository,
    private val giftedSubscriptionRepository: GiftedSubscriptionRepository,
    private val firebaseMessagingTokenRepository: FirebaseMessagingTokenRepository,
    private val templateRepository: RelationalTemplateRepository,
    private val checklistRepository: ChecklistRepository,
    private val templateEventRepository: EventRepository<TemplateEvent>,
    private val checklistEventRepository: EventRepository<ChecklistEvent>,
) : DeleteUserUseCase {

    context(AuthenticatedScope)
    override suspend fun deleteUser() {
        purchaseRepository.deleteUserPurchases()
        giftedSubscriptionRepository.deleteUserGifts()
        firebaseMessagingTokenRepository.deleteUserFirebaseTokens()
        val checklistAggregates = checklistRepository.readAll().map(Checklist::id)
        val templateAggregates = templateRepository.readAll().map(Template::id)
        checklistEventRepository.deleteStreamsForAggregates(checklistAggregates)
        templateEventRepository.deleteStreamsForAggregates(templateAggregates)
        checklistRepository.deleteUserChecklists()
        templateRepository.deleteUserTemplates()
        userService.delete()
        FirebaseAuth.getInstance().deleteUser(principal.userId)
    }
}
