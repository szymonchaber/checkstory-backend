package tech.checkstory.template

interface DomainCommand {

    val eventId: String
    val timestamp: Long
    val userId: String
}