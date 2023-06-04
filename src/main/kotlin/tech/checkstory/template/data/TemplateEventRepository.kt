package tech.checkstory.template.data

import com.eventstore.dbclient.EventStoreDBClient
import tech.checkstory.data.EventRepository
import tech.checkstory.data.EventStoreEventRepository
import tech.checkstory.template.domain.event.TemplateEvent

class TemplateEventRepositoryImpl(client: EventStoreDBClient) : EventStoreEventRepository<TemplateEvent>(client),
    TemplateEventRepository {

    override val streamNamePrefix = "checklist-template"

    override val knownEventTypes = setOf(
        "createTemplate",
        "editTemplateTitle",
        "editTemplateDescription",
        "addTemplateTask",
        "renameTemplateTask",
        "deleteTemplateTask",
        "updateTemplateTasksPositions",
        "moveTemplateTask",
        "addOrUpdateTemplateReminder",
        "deleteTemplateReminder",
        "deleteTemplate"
    )

    override fun getSerializer() = TemplateEvent.serializer()
}

interface TemplateEventRepository : EventRepository<TemplateEvent>
