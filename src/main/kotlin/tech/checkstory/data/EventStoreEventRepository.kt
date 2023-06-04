package tech.checkstory.data

import com.eventstore.dbclient.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import tech.checkstory.auth.user.AuthenticatedScope
import tech.checkstory.auth.user.User
import tech.checkstory.domain.command.DtoUUID
import tech.checkstory.domain.event.DomainEvent
import java.io.ByteArrayOutputStream
import java.util.*

abstract class EventStoreEventRepository<T : DomainEvent>(
    private val client: EventStoreDBClient,
    private val json: Json = Json {
        ignoreUnknownKeys = true
    }
) {

    abstract val streamNamePrefix: String

    abstract val knownEventTypes: Set<String>

    private val fullStreamNamePrefix by lazy { "$streamNamePrefix-" }

    suspend fun getAllGrouped(): Map<DtoUUID, List<T>> {
        val result = withContext(Dispatchers.IO) {
            client.readAll(ReadAllOptions.get().fromStart().forwards()).await()
        }
        return result.events
            .mapNotNull { deserializeEventWithAggregateId(it) }
            .groupBy(keySelector = Pair<DtoUUID, T>::first, valueTransform = Pair<DtoUUID, T>::second)
    }


    suspend fun getEventsForAggregate(id: UUID): List<T> {
        val result = withContext(Dispatchers.IO) {
            try {
                client.readStream("$fullStreamNamePrefix$id", readStreamOptions()).await().events
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

    private fun deserializeEventWithAggregateId(event: ResolvedEvent): Pair<DtoUUID, T>? {
        val eventType = event.event.eventType
        val eventData = event.event.eventData
        val streamId = event.event.streamId
        return if (eventType in knownEventTypes) {
            DtoUUID.fromString(streamId.removePrefix(fullStreamNamePrefix)) to decode(eventData)
        } else {
            null
        }
    }

    context(AuthenticatedScope)
    suspend fun save(event: T, aggregateId: DtoUUID) {
        val eventData = toEventData(event, principal)
        client.appendToStream("$fullStreamNamePrefix$aggregateId", eventData).await()
    }

    context(AuthenticatedScope)
    suspend fun saveAll(events: List<T>, aggregateId: DtoUUID) {
        val eventDataList = events.map {
            toEventData(it, principal)
        }
        client.appendToStream("$fullStreamNamePrefix$aggregateId", eventDataList.listIterator()).await()
    }

    private fun toEventData(event: T, user: User): EventData {
        val eventDataBinary = ByteArrayOutputStream().use { outputArray ->
            encode(event, outputArray)
            outputArray.toByteArray()
        }
        val metadata = ByteArrayOutputStream().use { outputArray ->
            encodeMetadata(EventMetadata(user.userId), outputArray)
            outputArray.toByteArray()
        }
        return EventData
            .builderAsBinary(
                event.eventId,
                event.eventType,
                eventDataBinary
            )
            .metadataAsBytes(metadata)
            .build()
    }

    @OptIn(ExperimentalSerializationApi::class)
    fun encodeMetadata(metadata: EventMetadata, outputStream: ByteArrayOutputStream) {
        Json.encodeToStream(metadata, outputStream)
    }

    @OptIn(ExperimentalSerializationApi::class)
    private fun decode(eventData: ByteArray): T {
        return json.decodeFromStream(getSerializer(), eventData.inputStream())
    }

    @OptIn(ExperimentalSerializationApi::class)
    private fun encode(event: T, outputStream: ByteArrayOutputStream) {
        json.encodeToStream(getSerializer(), event, outputStream)
    }

    abstract fun getSerializer(): KSerializer<T>
}

@Serializable
data class EventMetadata(val userId: String)
