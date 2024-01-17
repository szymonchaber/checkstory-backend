package tech.checkstory.domain.command

import kotlinx.datetime.Instant
import tech.checkstory.core.domain.DtoUUID

interface DomainCommand {

    val commandId: DtoUUID
    val timestamp: Instant
}

