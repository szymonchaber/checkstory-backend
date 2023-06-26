package tech.checkstory.eventbus

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import tech.checkstory.read.subscription.EnrichedEvent

class EventBus {

    private val _events = MutableSharedFlow<EnrichedEvent>()
    val events: SharedFlow<EnrichedEvent>
        get() = _events

    suspend fun publish(domainEvent: EnrichedEvent) {
        _events.emit(domainEvent)
    }
}
