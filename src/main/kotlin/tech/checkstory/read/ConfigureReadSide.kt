package tech.checkstory.read

import com.eventstore.dbclient.*
import io.ktor.server.application.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.future.await
import org.kodein.di.DI
import org.kodein.di.instance
import org.slf4j.LoggerFactory
import tech.checkstory.data.EventMapper
import tech.checkstory.eventbus.EventBus

context(DI)
fun Application.configureReadSide() {
    val eventBus by instance<EventBus>()
    val client by instance<EventStoreDBClient>()
    val mappers by instance<Set<EventMapper<*>>>()
    val subscription = EventStoreDBSubscriptionToAll(client, eventBus, mappers)
    repeat(10) { index ->
        CoroutineScope(Job()).launch {
            withContext(Dispatchers.Default) {
                eventBus.events
                    .collect {
                        log.debug("Consumer $index: a new event consumed!: ${it.eventType}")
                    }
            }
        }
    }
    CoroutineScope(Job()).launch {
        withContext(Dispatchers.Default) {
            subscription.subscribeToAll()
        }
    }
}

class EventStoreDBSubscriptionToAll(
    private val eventStoreClient: EventStoreDBClient,
//    private val checkpointRepository: SubscriptionCheckpointRepository,
    private val eventBus: EventBus,
    private val mappers: Set<EventMapper<*>>
) {
    private val logger = LoggerFactory.getLogger(EventStoreDBSubscriptionToAll::class.java)

    suspend fun subscribeToAll() {
        subscribeToAll(EventStoreDBSubscriptionToAllOptions.default)
    }

    private suspend fun subscribeToAll(subscriptionOptions: EventStoreDBSubscriptionToAllOptions) {
        callbackFlow {
            try {
//            val checkpoint = checkpointRepository.load(subscriptionOptions.subscriptionId)
                val checkpoint: Long? = null
                if (checkpoint != null) {
                    subscriptionOptions.subscribeToAllOptions
                        .fromPosition(Position(checkpoint, checkpoint))
                } else {
                    subscriptionOptions.subscribeToAllOptions.fromStart()
                }
                logger.info("Subscribing to all ${subscriptionOptions.subscriptionId}")
                val subscription = eventStoreClient.subscribeToAll(
                    object : SubscriptionListener() {

                        override fun onEvent(subscription: Subscription, event: ResolvedEvent) {
                            try {
                                trySend(event)
                            } catch (throwable: Throwable) {
                                logger.error("Error while handling event", throwable)
                                throw RuntimeException(throwable)
                            }
                        }

                        override fun onError(subscription: Subscription, throwable: Throwable) {
                            logger.error("Subscription was dropped", throwable)
                            throw RuntimeException(throwable)
                        }
                    },
                    subscriptionOptions.subscribeToAllOptions
                ).await()

                awaitClose {
                    subscription.stop()
                }
            } catch (e: Throwable) {
                logger.error("Error while starting subscription", e)
                throw RuntimeException(e)
            }
        }
            .onEach {
                handleEvent(it, subscriptionOptions.subscriptionId)
            }
            .collect()
    }

    private suspend fun handleEvent(resolvedEvent: ResolvedEvent, subscriptionId: String) {
        mappers
            .firstNotNullOf {
                if (it.knownEventTypes.contains(resolvedEvent.event.eventType)) {
                    it.deserializeEvent(resolvedEvent)
                } else {
                    null
                }
            }.let {
                eventBus.publish(it)
                logger.debug("Setting checkpoint at ${resolvedEvent.event.position.commitUnsigned}")
//                checkpointRepository.store(
//                     subscriptionId,
//                    resolvedEvent.event.position.commitUnsigned
//                )
            }
    }
}

interface SubscriptionCheckpointRepository {

    suspend fun load(subscriptionId: String): Long?

    suspend fun store(subscriptionId: String, position: Long)
}


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
