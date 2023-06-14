package tech.checkstory.template.data

import com.eventstore.dbclient.EventStoreDBClient
import tech.checkstory.data.EventStoreEventRepository
import tech.checkstory.data.InMemoryEventRepository
import tech.checkstory.template.domain.event.TemplateEvent

class TemplateEventRepositoryImpl(
    client: EventStoreDBClient,
    mapper: TemplateEventMapper
) : EventStoreEventRepository<TemplateEvent>(client, mapper)

class InMemoryTemplateEventRepository : InMemoryEventRepository<TemplateEvent>()
