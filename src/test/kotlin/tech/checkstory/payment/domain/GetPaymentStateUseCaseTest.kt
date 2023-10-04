@file:Suppress("INLINE_FROM_HIGHER_PLATFORM")

package tech.checkstory.payment.domain

import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Instant
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.*
import tech.checkstory.payment.data.GiftedSubscriptionRepository
import tech.checkstory.payment.data.Purchase
import tech.checkstory.payment.data.PurchaseRepository

class GetPaymentStateUseCaseTest {

    private val giftedSubscriptionRepository = mock<GiftedSubscriptionRepository>()

    private val purchaseRepository = mock<PurchaseRepository>()

    private val storeGooglePlayTokenUseCase = mock<StoreGooglePlayTokenUseCase>()

    private val useCaseTest =
        GetPaymentStateUseCase(purchaseRepository, giftedSubscriptionRepository, storeGooglePlayTokenUseCase)

    @Test
    fun `given gift subscription, when getPaymentStateForUser called, then should return true`() =
        runBlocking {
            // given
            whenever(giftedSubscriptionRepository.hasSubscriptionGift(USER_ID)).thenReturn(true)

            // when
            val paymentStateForUser = useCaseTest.getPaymentStateForUser(USER_ID)

            // then
            assertTrue(paymentStateForUser.isSubscriptionActive)
        }

    @Test
    fun `given gift subscription, when getPaymentStateForUser called, then should not check in payment repository`() =
        runBlocking {
            // given
            whenever(giftedSubscriptionRepository.hasSubscriptionGift(USER_ID)).thenReturn(true)

            // when
            useCaseTest.getPaymentStateForUser(USER_ID)

            // then
            verifyNoInteractions(purchaseRepository)
        }

    @Test
    fun `given no gift or normal subscription, when getPaymentStateForUser called, then should check in purchases once only`(): Unit =
        runBlocking {
            // given
            whenever(giftedSubscriptionRepository.hasSubscriptionGift(USER_ID)).thenReturn(false)
            wheneverBlocking { purchaseRepository.getByUserId(USER_ID) } doReturn listOf()

            // when
            useCaseTest.getPaymentStateForUser(USER_ID)

            // then
            verify(purchaseRepository).getByUserId(USER_ID)
        }

    @Test
    fun `given unexpired subscription, when getPaymentStateForUser called, then should return true`(): Unit =
        runBlocking {
            // given
            whenever(giftedSubscriptionRepository.hasSubscriptionGift(USER_ID)).thenReturn(false)
            wheneverBlocking { purchaseRepository.getByUserId(USER_ID) } doReturn listOf(
                Purchase(Instant.DISTANT_FUTURE, googlePlayPaymentToken)
            )

            // when
            val paymentStateForUser = useCaseTest.getPaymentStateForUser(USER_ID)

            // then
            assertTrue(paymentStateForUser.isSubscriptionActive)
        }

    @Test
    fun `given expired subscription, when getPaymentStateForUser called, then should refresh subscription state and check again`(): Unit =
        runBlocking {
            // given
            whenever(giftedSubscriptionRepository.hasSubscriptionGift(USER_ID)).thenReturn(false)
            wheneverBlocking { purchaseRepository.getByUserId(USER_ID) } doReturnConsecutively (listOf(
                listOf(Purchase(Instant.DISTANT_PAST, googlePlayPaymentToken)),
                listOf(Purchase(Instant.DISTANT_FUTURE, googlePlayPaymentToken))
            ))

            // when
            val paymentState = useCaseTest.getPaymentStateForUser(USER_ID)

            // then
            verify(storeGooglePlayTokenUseCase).storePaymentToken(USER_ID, googlePlayPaymentToken)
            verify(purchaseRepository, times(2)).getByUserId(USER_ID)
            assertTrue(paymentState.isSubscriptionActive)
        }

    companion object {

        private const val USER_ID = "userId"
        private val googlePlayPaymentToken = GooglePlayPaymentToken("PAYMENT_TOKEN")
    }
}
