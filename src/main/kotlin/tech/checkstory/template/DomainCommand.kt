package tech.checkstory.template

import kotlinx.serialization.Serializable

@Serializable
sealed interface DomainCommand {

    val eventId: String
    val timestamp: Long
    val userId: String
}