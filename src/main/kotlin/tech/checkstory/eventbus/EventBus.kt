package tech.checkstory.eventbus

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import tech.checkstory.domain.event.DomainEvent

class EventBus {

    private val _events = MutableSharedFlow<DomainEvent>()
    val events: SharedFlow<DomainEvent>
        get() = _events

    suspend fun publish(domainEvent: DomainEvent) {
        _events.emit(domainEvent)
    }
}
