package tech.checkstory.domain.event

import kotlinx.datetime.Instant
import tech.checkstory.core.domain.DtoUUID

interface DomainEvent {

    val aggregateId: DtoUUID
    val timestamp: Instant
}
