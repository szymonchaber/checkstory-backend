package tech.checkstory.domain.event

import kotlinx.datetime.Instant
import kotlinx.serialization.Transient
import tech.checkstory.domain.command.DtoUUID

interface DomainEvent {

    @Transient
    val eventType: String
    val aggregateId: DtoUUID
    val eventId: DtoUUID
    val timestamp: Instant
}

interface CreationEvent

interface MutationEvent
