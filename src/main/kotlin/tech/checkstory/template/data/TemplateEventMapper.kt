package tech.checkstory.template.data

import kotlinx.serialization.json.Json
import tech.checkstory.data.EventMapper
import tech.checkstory.template.domain.event.TemplateEvent

class TemplateEventMapper : EventMapper<TemplateEvent>(
    Json {
        ignoreUnknownKeys = true
    }
) {

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
