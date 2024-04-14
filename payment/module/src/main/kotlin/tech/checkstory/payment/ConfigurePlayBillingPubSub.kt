package tech.checkstory.payment

import io.ktor.server.application.*
import org.kodein.di.DI
import org.kodein.di.instance
import tech.checkstory.payment.playbilling.PlayBillingPubSubHandler

context(DI)
fun Application.configurePlayBillingPubSub() {
    val playBillingPubSubHandler by instance<PlayBillingPubSubHandler>()
    playBillingPubSubHandler.subscribeToBillingMessages()
}
