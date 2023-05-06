package tech.checkstory.data

import com.eventstore.dbclient.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withContext
import tech.checkstory.domain.command.DtoUUID
import tech.checkstory.domain.event.DomainEvent
import java.io.ByteArrayOutputStream
import java.util.*

abstract class EventRepository<T : DomainEvent>(private val client: EventStoreDBClient) {

    abstract val streamNamePrefix: String

    abstract val knownEventTypes: Set<String>

    abstract suspend fun save(event: T)

    suspend fun getAll(): List<T> {
        val result = withContext(Dispatchers.IO) {
            client.readAll(ReadAllOptions.get().fromStart().forwards()).await()
        }
        return result.events
            .mapNotNull(::deserializeEvent)
    }

    suspend fun getEventsForAggregate(id: UUID): List<T> {
        val result = withContext(Dispatchers.IO) {
            try {
                client.readStream("$streamNamePrefix-$id", readStreamOptions()).await().events
            } catch (exception: StreamNotFoundException) {
                listOf()
            }
        }
        return result.mapNotNull(::deserializeEvent)
    }


    private fun readStreamOptions(): ReadStreamOptions {
        return ReadStreamOptions.get()
            .forwards()
            .fromStart()
    }

    private fun deserializeEvent(event: ResolvedEvent): T? {
        val eventType = event.event.eventType
        val eventData = event.event.eventData
        return if (eventType in knownEventTypes) {
            decode(eventData)
        } else {
            null
        }
    }

    protected suspend fun save(event: T, id: DtoUUID) {
        val eventData = ByteArrayOutputStream().use { outputArray ->
            encode(event, outputArray)
            EventData
                .builderAsBinary(
                    event.eventId,
                    event.eventType,
                    outputArray.toByteArray()
                ) // event type gets stored within dto - maybe not good!
                .build()
        }

        client.appendToStream("$streamNamePrefix-$id", eventData).await()
    }

    abstract fun encode(event: T, outputStream: ByteArrayOutputStream)

    abstract fun decode(eventData: ByteArray): T
}