package tech.checkstory.data

import com.eventstore.dbclient.EventStoreDBClient
import com.eventstore.dbclient.ReadStreamOptions
import tech.checkstory.checklist.domain.event.ChecklistEvent
import tech.checkstory.domain.event.DomainEvent
import java.util.*

abstract class EventRepository<T : DomainEvent>(protected val client: EventStoreDBClient) {

    abstract fun save(event: T)

    abstract suspend fun getAll(): List<T>

    abstract suspend fun getEventsForAggregate(id: UUID): List<ChecklistEvent>

    protected fun readStreamOptions(): ReadStreamOptions {
        return ReadStreamOptions.get()
            .forwards()
            .fromStart()
    }
}