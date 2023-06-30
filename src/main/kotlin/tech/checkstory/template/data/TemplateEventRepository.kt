package tech.checkstory.template.data

import tech.checkstory.data.EventStoreConnectionManager
import tech.checkstory.data.EventStoreEventRepository
import tech.checkstory.data.InMemoryEventRepository
import tech.checkstory.template.domain.event.TemplateEvent

class TemplateEventRepositoryImpl(
    connectionManager: EventStoreConnectionManager,
    mapper: TemplateEventMapper
) : EventStoreEventRepository<TemplateEvent>(connectionManager, mapper)

class InMemoryTemplateEventRepository : InMemoryEventRepository<TemplateEvent>()
