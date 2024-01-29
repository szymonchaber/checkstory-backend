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

    context(AuthenticatedScope)
    override suspend fun deleteStreamsForAggregates(aggregateIds: List<DtoUUID>) {
        aggregateIds.map {
            val streamName = "${eventMapper.fullStreamNamePrefix}$it"
            println("Deleting stream: $streamName")
            connectionManager.getClient()
                .tombstoneStream(streamName)
                .await()
        }
    }

    private fun createUserIdProperties(mutableMap: MutableMap<String, Any>, userId: String): MutableMap<String, Any> {
        return mutableMap.apply {
            this[STREAM_PROPERTY_USER_ID] = userId
        }
    }

    companion object {

        private const val STREAM_PROPERTY_USER_ID = "userId"
    }
}

@Serializable
data class EventMetadata(val userId: String)
