package tech.checkstory.checklist.data

import com.eventstore.dbclient.EventStoreDBClient
import tech.checkstory.checklist.domain.event.ChecklistEvent
import tech.checkstory.data.EventStoreEventRepository
import tech.checkstory.data.InMemoryEventRepository

class ChecklistEventRepositoryImpl(
    client: EventStoreDBClient,
    mapper: ChecklistEventMapper
) : EventStoreEventRepository<ChecklistEvent>(client, mapper)

class InMemoryChecklistEventRepository : InMemoryEventRepository<ChecklistEvent>()
