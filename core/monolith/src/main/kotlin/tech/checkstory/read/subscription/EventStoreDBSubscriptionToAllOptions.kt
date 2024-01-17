package tech.checkstory.read.subscription

import com.eventstore.dbclient.SubscribeToAllOptions
import com.eventstore.dbclient.SubscriptionFilter

data class EventStoreDBSubscriptionToAllOptions(
    val subscriptionId: String,
    val subscribeToAllOptions: SubscribeToAllOptions
) {

    companion object {
        val default: EventStoreDBSubscriptionToAllOptions
            get() {
                val filterOutSystemEvents = SubscriptionFilter.newBuilder()
                    .withEventTypeRegularExpression("^[^\\$].*")
                    .build()
                val options = SubscribeToAllOptions.get()
                    .fromStart()
                    .filter(filterOutSystemEvents)
                return EventStoreDBSubscriptionToAllOptions("default", options)
            }
    }
}
