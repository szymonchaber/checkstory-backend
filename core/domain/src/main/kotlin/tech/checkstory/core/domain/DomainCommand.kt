package tech.checkstory.core.domain

import kotlinx.datetime.Instant

interface DomainCommand {

    val commandId: DtoUUID
    val timestamp: Instant
}

