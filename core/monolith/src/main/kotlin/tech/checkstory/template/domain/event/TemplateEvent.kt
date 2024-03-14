package tech.checkstory.template.domain.event

import kotlinx.datetime.Instant
import tech.checkstory.core.domain.DtoUUID
import tech.checkstory.domain.event.DomainEvent
import tech.checkstory.template.domain.command.ExistingTemplateDataDto

sealed interface TemplateEvent : DomainEvent {

    data class CreateTemplateEvent(
        override val aggregateId: DtoUUID,
        override val timestamp: Instant,
        val userId: String,
        val existingTemplateData: ExistingTemplateDataDto?
    ) : TemplateEvent
}
