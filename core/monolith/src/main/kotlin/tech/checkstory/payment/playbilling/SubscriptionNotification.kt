package tech.checkstory.payment.playbilling

import kotlinx.serialization.Serializable

@Serializable
data class SubscriptionNotification(
    val version: String,
    val notificationType: Int,
    val purchaseToken: String,
    val subscriptionId: String
)
