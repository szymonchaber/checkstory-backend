package tech.checkstory.data

import tech.checkstory.core.domain.DtoUUID
import tech.checkstory.core.domain.auth.AuthenticatedScope
import tech.checkstory.domain.event.DomainEvent
import java.util.*

interface EventRepository<T : DomainEvent> {

    suspend fun getEventsForAggregate(id: UUID): List<T>

    context(AuthenticatedScope)
    suspend fun saveAll(events: List<T>, aggregateId: DtoUUID)

    context(AuthenticatedScope)
    suspend fun deleteStreamsForAggregates(aggregateIds: List<DtoUUID>)
}
