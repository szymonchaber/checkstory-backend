package tech.checkstory.eventbus

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import tech.checkstory.read.subscription.EventEnvelope

class EventBus {

    private val _events = MutableSharedFlow<EventEnvelope>()
    val events: SharedFlow<EventEnvelope>
        get() = _events

    suspend fun publish(domainEvent: EventEnvelope) {
        _events.emit(domainEvent)
    }
}
