package tech.checkstory.read.subscription

import com.eventstore.dbclient.Position
import com.eventstore.dbclient.ResolvedEvent
import com.eventstore.dbclient.Subscription
import com.eventstore.dbclient.SubscriptionListener
import io.sentry.Sentry
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.future.await
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import tech.checkstory.data.EventMapper
import tech.checkstory.data.EventMetadata
import tech.checkstory.data.EventStoreConnectionManager
import tech.checkstory.domain.event.DomainEvent
import tech.checkstory.eventbus.EventBus
import kotlin.math.min
import kotlin.math.pow

class EventStoreDBSubscriptionToAll(
    private val connectionManager: EventStoreConnectionManager,
    private val checkpointRepository: SubscriptionCheckpointRepository,
    private val eventBus: EventBus,
    private val mappers: Set<EventMapper<*>>
) : SubscriptionToAll {

    private val logger = LoggerFactory.getLogger(EventStoreDBSubscriptionToAll::class.java)

    override suspend fun subscribeToAll() {
        subscribeToAll(EventStoreDBSubscriptionToAllOptions.default)
    }

    private suspend fun subscribeToAll(subscriptionOptions: EventStoreDBSubscriptionToAllOptions) {
        callbackFlow {
            var subscription: Subscription? = null
            try {
                logger.info("Starting / restarting subscription")
                val checkpoint = checkpointRepository.load(subscriptionOptions.subscriptionId)
                if (checkpoint != null) {
                    subscriptionOptions.subscribeToAllOptions
                        .fromPosition(Position(checkpoint, checkpoint))
                } else {
                    subscriptionOptions.subscribeToAllOptions.fromStart()
                }
                logger.info("Subscribing to all ${subscriptionOptions.subscriptionId} from position $checkpoint")
                subscription = connectionManager.getClient().subscribeToAll(
                    object : SubscriptionListener() {

                        override fun onEvent(subscription: Subscription, event: ResolvedEvent) {
                            try {
                                trySend(event)
                            } catch (throwable: Throwable) {
                                logger.error("Error while handling event", throwable)
                                close(throwable)
                            }
                        }

                        override fun onCancelled(subscription: Subscription?, exception: Throwable?) {
                            logger.error("Subscription was dropped", exception)
                            close(exception)
                        }
                    },
                    subscriptionOptions.subscribeToAllOptions
                ).await()
            } catch (throwable: Throwable) {
                logger.error("Error while starting subscription", throwable)
                close(throwable)
            }
            awaitClose {
                subscription?.stop()
            }
        }
            .buffer(capacity = Channel.Factory.UNLIMITED)
            .retryExponential(
                initialDelay = 100L,
                maxDelay = 5000L,
                logger
            )
            .onEach {
                handleEvent(it, subscriptionOptions.subscriptionId)
            }
            .collect()
    }

    private suspend fun handleEvent(resolvedEvent: ResolvedEvent, subscriptionId: String) {
        mappers
            .firstNotNullOfOrNull {
                if (it.knownEventTypes.contains(resolvedEvent.event.eventType)) {
                    it.deserializeEventWithMetadata(resolvedEvent)?.let { (event, metadata) ->
                        EventEnvelope(event, metadata)
                    }
                } else {
                    null
                }
            }?.let {
                logger.debug("Publishing event with id ${resolvedEvent.event.eventId}")
                eventBus.publish(it)
                logger.debug("Setting checkpoint at ${resolvedEvent.event.position.commitUnsigned}")
                checkpointRepository.store(
                    subscriptionId,
                    resolvedEvent.event.position.commitUnsigned
                )
            }
    }
}

data class EventEnvelope(val event: DomainEvent, val metadata: EventMetadata)

fun <T> Flow<T>.retryExponential(
    initialDelay: Long,
    maxDelay: Long,
    logger: Logger
): Flow<T> {
    return retryWhen { cause, attempt ->
        val interval = (initialDelay * 2.toDouble().pow(attempt.toDouble())).toLong()
        delay(min(maxDelay, interval))
        logger.error("Flow failed", cause)
        Sentry.captureException(cause)
        return@retryWhen true
    }
}
