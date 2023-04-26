package tech.checkstory.template

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
sealed interface DomainEvent {

    @Transient
    val eventType: String
    val eventId: String
    val timestamp: Long
    val userId: String
}