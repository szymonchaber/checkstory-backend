package tech.checkstory.data

import tech.checkstory.auth.user.AuthenticatedScope
import tech.checkstory.domain.command.DtoUUID
import tech.checkstory.domain.event.DomainEvent
import java.util.*

abstract class InMemoryEventRepository<T : DomainEvent> : EventRepository<T> {

    private val aggregateIdToEventList = mutableMapOf<DtoUUID, List<T>>()

    override suspend fun getAllGrouped(): Map<DtoUUID, List<T>> {
        return aggregateIdToEventList
    }

    override suspend fun getEventsForAggregate(id: UUID): List<T> {
        return aggregateIdToEventList.getOrDefault(id, listOf())
    }

    context(AuthenticatedScope)
    override suspend fun saveAll(events: List<T>, aggregateId: DtoUUID) {
        aggregateIdToEventList.merge(aggregateId, events) { old, new ->
            old.plus(new)
        }
    }

    context(AuthenticatedScope)
    override suspend fun save(event: T, aggregateId: DtoUUID) {
        aggregateIdToEventList.merge(aggregateId, listOf(event)) { old, new ->
            old.plus(new)
        }
    }
}
