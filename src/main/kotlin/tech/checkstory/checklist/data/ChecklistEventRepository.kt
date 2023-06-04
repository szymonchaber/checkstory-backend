package tech.checkstory.checklist.data

import com.eventstore.dbclient.EventStoreDBClient
import tech.checkstory.checklist.domain.event.ChecklistEvent
import tech.checkstory.data.EventRepository
import tech.checkstory.data.EventStoreEventRepository

class ChecklistEventRepositoryImpl(client: EventStoreDBClient) : EventStoreEventRepository<ChecklistEvent>(client),
    ChecklistEventRepository {

    override val streamNamePrefix = "checklist"

    override val knownEventTypes = setOf(
        "createChecklist",
        "editChecklistNotes",
        "changeTaskChecked",
        "deleteChecklist"
    )

    override fun getSerializer() = ChecklistEvent.serializer()
}

interface ChecklistEventRepository : EventRepository<ChecklistEvent>
