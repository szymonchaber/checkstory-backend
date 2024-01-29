package tech.checkstory.data

import com.eventstore.dbclient.ReadStreamOptions
import com.eventstore.dbclient.StreamNotFoundException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import tech.checkstory.core.domain.DtoUUID
import tech.checkstory.core.domain.auth.AuthenticatedScope
import tech.checkstory.domain.event.DomainEvent
import java.util.*

abstract class EventStoreEventRepository<T : DomainEvent>(
    private val connectionManager: EventStoreConnectionManager,
    private val eventMapper: EventMapper<T>,
) : EventRepository<T> {

    override suspend fun getEventsForAggregate(id: UUID): List<T> {
        val result = withContext(Dispatchers.IO) {
            try {
                connectionManager.getClient().readStream("${eventMapper.fullStreamNamePrefix}$id", readStreamOptions())
                    .await().events
            } catch (exception: StreamNotFoundException) {
                listOf()
            }
        }
        return result.mapNotNull(eventMapper::deserializeEvent)
    }

    private fun readStreamOptions(): ReadStreamOptions {
        return ReadStreamOptions.get()
            .forwards()
            .fromStart()
    }

    context(AuthenticatedScope)
    override suspend fun saveAll(events: List<T>, aggregateId: DtoUUID) {
        val eventDataList = events.map {
            eventMapper.toEventData(it, principal)
        }
        connectionManager.getClient()
            .appendToStream("${eventMapper.fullStreamNamePrefix}$aggregateId", eventDataList.listIterator())
            .await()
    }
}

@Serializable
data class EventMetadata(val userId: String)
