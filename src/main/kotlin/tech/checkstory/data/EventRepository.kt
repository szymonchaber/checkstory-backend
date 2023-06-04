package tech.checkstory.data

import tech.checkstory.auth.user.AuthenticatedScope
import tech.checkstory.domain.command.DtoUUID
import tech.checkstory.domain.event.DomainEvent
import java.util.*

interface EventRepository<T : DomainEvent> {

    suspend fun getAllGrouped(): Map<DtoUUID, List<T>>

    suspend fun getEventsForAggregate(id: UUID): List<T>
    context(AuthenticatedScope)
    suspend fun save(event: T, aggregateId: DtoUUID)

    context(AuthenticatedScope)
    suspend fun saveAll(events: List<T>, aggregateId: DtoUUID)
}
