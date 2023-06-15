package tech.checkstory.read.subscription

interface SubscriptionCheckpointRepository {

    suspend fun load(subscriptionId: String): Long?

    suspend fun store(subscriptionId: String, position: Long)
}
