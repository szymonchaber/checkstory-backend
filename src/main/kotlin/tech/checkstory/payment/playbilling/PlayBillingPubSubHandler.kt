package tech.checkstory.payment.playbilling

import com.google.cloud.pubsub.v1.AckReplyConsumer
import com.google.cloud.pubsub.v1.MessageReceiver
import com.google.cloud.pubsub.v1.Subscriber
import com.google.pubsub.v1.ProjectSubscriptionName
import com.google.pubsub.v1.PubsubMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import tech.checkstory.notification.firebase.UserDataChangeNotifier
import tech.checkstory.payment.data.PurchaseRepository
import tech.checkstory.payment.data.google_play.SubscriptionsGoogleApi
import tech.checkstory.projectId
import tech.checkstory.subscriptionId
import java.io.ByteArrayInputStream

@OptIn(ExperimentalSerializationApi::class)
class PlayBillingPubSubHandler(
    private val subscriptionsGoogleApi: SubscriptionsGoogleApi,
    private val purchaseRepository: PurchaseRepository,
    private val notifier: UserDataChangeNotifier,
    private val json: Json = Json {
        this.explicitNulls = false
    }
) {

    init {
        GlobalScope.launch {
            withContext(Dispatchers.Default) {
                subscribeAsyncExample(projectId, subscriptionId)
            }
        }
    }

    private fun subscribeAsyncExample(projectId: String, subscriptionId: String) {
        val subscriptionName = ProjectSubscriptionName.of(projectId, subscriptionId)

        val receiver = MessageReceiver { message: PubsubMessage, consumer: AckReplyConsumer ->
            GlobalScope.launch {
                println("Id: " + message.messageId)
                println("Data: " + message.data.toStringUtf8())
                try {
                    handle(message, consumer)
                } catch (exception: Exception) {
                    println(exception)
                    consumer.nack()
                }
            }
        }
        val subscriber = Subscriber.newBuilder(subscriptionName, receiver).build()
        try {
            subscriber.startAsync().awaitTerminated()
        } catch (exception: Exception) {
            println(exception)
            subscriber.stopAsync()
        }
    }

    private suspend fun handle(
        message: PubsubMessage,
        consumer: AckReplyConsumer
    ) {
        val notification = json.decodeFromStream(
            PlayBillingNotification.serializer(),
            ByteArrayInputStream(message.data.toByteArray())
        )
        println("New billing notification: $notification")
        notification.subscriptionNotification
            ?.let {
                try {
                    handleSubscriptionNotification(it)
                } catch (exception: Exception) {
                    consumer.nack()
                }
            }
    }

    private suspend fun handleSubscriptionNotification(notification: SubscriptionNotification) {
        if (notification.notificationType == NotificationType.SUBSCRIPTION_PURCHASED.intValue) {
            return
        }
        val rawValue = subscriptionsGoogleApi.getSubscription(notification.purchaseToken).rawValue
        purchaseRepository.updatePurchase(notification.purchaseToken, rawValue)
        val user = purchaseRepository.getUserIdWithToken(notification.purchaseToken)
        user?.let {
            notifier.notifyUserOfPaymentStatusChange(it)
        }
    }
}
