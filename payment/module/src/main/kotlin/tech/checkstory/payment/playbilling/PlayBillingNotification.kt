package tech.checkstory.payment.playbilling

import kotlinx.serialization.Serializable

@Serializable
data class PlayBillingNotification(
    val version: String,
    val packageName: String,
    val eventTimeMillis: Long,
    val subscriptionNotification: SubscriptionNotification?,
    val testNotification: TestNotification?
)
