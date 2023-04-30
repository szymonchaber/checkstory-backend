package tech.checkstory.template

import kotlinx.serialization.Transient

interface DomainEvent {

    @Transient
    val eventType: String
    val eventId: DtoUUID
    val timestamp: Long
    val userId: String
}