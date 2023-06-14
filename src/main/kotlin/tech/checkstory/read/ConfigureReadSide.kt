package tech.checkstory.read

import com.eventstore.dbclient.EventStoreDBClient
import io.ktor.server.application.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.datetime.Clock
import org.kodein.di.DI
import org.kodein.di.instance
import tech.checkstory.checklist.domain.event.ChecklistEvent
import tech.checkstory.eventbus.EventBus
import java.util.*

context(DI)
fun Application.configureReadSide() {
    val eventBus by instance<EventBus>()
    val client by instance<EventStoreDBClient>()
    repeat(10) { index ->
        CoroutineScope(Job()).launch {
            withContext(Dispatchers.Default) {
                eventBus.events.filterIsInstance<ChecklistEvent>()
                    .collect {
                        log.debug("Consumer $index: a new checklist event consumed!")
                    }
            }
        }
    }
    CoroutineScope(Job()).launch {
        withContext(Dispatchers.Default) {
            generateSequence(0) { it + 1 }
                .forEach { i ->
                    log.debug("Producing a nice event number $i")
                    eventBus.publish(
                        ChecklistEvent.CreateChecklistEvent(
                            templateId = UUID.randomUUID(),
                            title = "New checklist",
                            description = "Description",
                            tasks = listOf(),
                            userId = "",
                            notes = "",
                            eventId = UUID.randomUUID(),
                            timestamp = Clock.System.now()
                        )
                    )
                    delay(1000)
                }
//            client.subscribeToAll(
//                object : SubscriptionListener() {
//
//                    override fun onEvent(subscription: Subscription?, event: ResolvedEvent?) {
//                        super.onEvent(subscription, event)
//                    }
//
//                    override fun onError(subscription: Subscription?, throwable: Throwable?) {
//                        super.onError(subscription, throwable)
//                    }
//
//                    override fun onCancelled(subscription: Subscription?) {
//                        super.onCancelled(subscription)
//                    }
//                },
//                SubscribeToAllOptions.get().fromStart()
//            )
        }
    }
}
