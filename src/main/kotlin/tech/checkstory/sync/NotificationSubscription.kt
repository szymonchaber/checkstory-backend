package tech.checkstory.sync

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import tech.checkstory.eventbus.EventBus
import tech.checkstory.notification.firebase.UserDataChangeNotifier
import tech.checkstory.read.subscription.EnrichedEvent
import java.util.concurrent.ConcurrentHashMap

class NotificationSubscription(
    private val eventBus: EventBus,
    private val notifier: UserDataChangeNotifier
) {

    private val userIdToNotificationJob = ConcurrentHashMap<String, Job>()
    private val notifierScope = CoroutineScope(Dispatchers.IO)

    suspend fun run() {
        withContext(Dispatchers.Default) {
            eventBus
                .events
                .onEach(::handleEvent)
                .collect()
        }
    }

    private suspend fun handleEvent(enrichedEvent: EnrichedEvent) {
        val userId = enrichedEvent.metadata.userId
        val job = userIdToNotificationJob[userId]
        if (job != null) {
            println("Cancelling user data change notification to debounce the event")
            job.cancel()
        }
        userIdToNotificationJob[userId] = notifierScope.launch(Dispatchers.IO) {
            println("Scheduling user data change notification")
            delay(1000)
            notifier.notifyUserOfDataChange(userId)
            userIdToNotificationJob.remove(userId)
        }
    }
}
