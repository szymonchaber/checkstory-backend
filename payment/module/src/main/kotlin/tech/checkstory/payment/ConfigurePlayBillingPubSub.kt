package tech.checkstory.payment

import io.ktor.server.application.*
import org.kodein.di.DI
import org.kodein.di.instance
import tech.checkstory.notification.firebase.UserDataChangeNotifier
import tech.checkstory.payment.data.PurchaseRepository
import tech.checkstory.payment.data.google_play.SubscriptionsGoogleApi
import tech.checkstory.payment.playbilling.PlayBillingPubSubHandler

context(DI)
fun Application.configurePlayBillingPubSub() {
    val subscriptionsGoogleApi by instance<SubscriptionsGoogleApi>()
    val purchaseRepository by instance<PurchaseRepository>()
    val userDataChangeNotifier by instance<UserDataChangeNotifier>()
    PlayBillingPubSubHandler(subscriptionsGoogleApi, purchaseRepository, userDataChangeNotifier)
}
