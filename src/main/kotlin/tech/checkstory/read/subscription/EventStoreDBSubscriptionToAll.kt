package tech.checkstory.read.subscription

import com.eventstore.dbclient.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.future.await
import org.slf4j.LoggerFactory
import tech.checkstory.data.EventMapper
import tech.checkstory.data.EventMetadata
import tech.checkstory.domain.event.DomainEvent
import tech.checkstory.eventbus.EventBus

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
                    it.deserializeEventWithMetadata(resolvedEvent)?.let { (event, metadata) ->
                        EnrichedEvent(event, metadata)
                    }
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

data class EnrichedEvent(val event: DomainEvent, val metadata: EventMetadata) : DomainEvent by event
