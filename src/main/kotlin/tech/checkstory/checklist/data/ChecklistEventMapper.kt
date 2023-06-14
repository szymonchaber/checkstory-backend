package tech.checkstory.checklist.data

import kotlinx.serialization.json.Json
import tech.checkstory.checklist.domain.event.ChecklistEvent
import tech.checkstory.data.EventMapper

class ChecklistEventMapper : EventMapper<ChecklistEvent>(
    Json {
        ignoreUnknownKeys = true
    }
) {

    override val streamNamePrefix = "checklist"

    override val knownEventTypes = setOf(
        "createChecklist",
        "editChecklistNotes",
        "changeTaskChecked",
        "deleteChecklist"
    )

    override fun getSerializer() = ChecklistEvent.serializer()
}
