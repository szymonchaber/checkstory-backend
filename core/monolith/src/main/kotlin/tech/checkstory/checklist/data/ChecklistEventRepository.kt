package tech.checkstory.checklist.data

import tech.checkstory.checklist.domain.event.ChecklistEvent
import tech.checkstory.data.EventStoreConnectionManager
import tech.checkstory.data.EventStoreEventRepository
import tech.checkstory.data.InMemoryEventRepository

class ChecklistEventRepositoryImpl(
    connectionManager: EventStoreConnectionManager,
    mapper: ChecklistEventMapper
) : EventStoreEventRepository<ChecklistEvent>(connectionManager, mapper)

class InMemoryChecklistEventRepository : InMemoryEventRepository<ChecklistEvent>()
