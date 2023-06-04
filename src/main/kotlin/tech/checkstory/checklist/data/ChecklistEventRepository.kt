package tech.checkstory.checklist.data

import com.eventstore.dbclient.EventStoreDBClient
import tech.checkstory.checklist.domain.event.ChecklistEvent
import tech.checkstory.data.EventStoreEventRepository
import tech.checkstory.data.InMemoryEventRepository

class ChecklistEventRepositoryImpl(client: EventStoreDBClient) : EventStoreEventRepository<ChecklistEvent>(client) {

    override val streamNamePrefix = "checklist"

    override val knownEventTypes = setOf(
        "createChecklist",
        "editChecklistNotes",
        "changeTaskChecked",
        "deleteChecklist"
    )

    override fun getSerializer() = ChecklistEvent.serializer()
}

class InMemoryChecklistEventRepository : InMemoryEventRepository<ChecklistEvent>()
