package tech.checkstory.data

import com.eventstore.dbclient.EventData
import com.eventstore.dbclient.ResolvedEvent
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import tech.checkstory.auth.user.User
import tech.checkstory.domain.command.DtoUUID
import tech.checkstory.domain.event.DomainEvent
import java.io.ByteArrayOutputStream
import java.util.*

abstract class EventMapper<T : DomainEvent>(
    private val json: Json
) {

    abstract val knownEventTypes: Set<String>

    fun toEventData(event: T, user: User): EventData {
        val eventDataBinary = ByteArrayOutputStream().use { outputArray ->
            encode(event, outputArray)
            outputArray.toByteArray()
        }
        val metadata = ByteArrayOutputStream().use { outputArray ->
            encodeMetadata(EventMetadata(user.userId), outputArray)
            outputArray.toByteArray()
        }
        return EventData.builderAsBinary(
            event.eventId,
            event.eventType,
            eventDataBinary
        )
            .metadataAsBytes(metadata)
            .build()
    }

    fun deserializeEvent(event: ResolvedEvent): T? {
        val eventType = event.event.eventType
        val eventData = event.event.eventData
        return if (eventType in knownEventTypes) {
            decode(eventData)
        } else {
            null
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    fun encodeMetadata(metadata: EventMetadata, outputStream: ByteArrayOutputStream) {
        json.encodeToStream(metadata, outputStream)
    }

    @OptIn(ExperimentalSerializationApi::class)
    fun decode(eventData: ByteArray): T {
        return json.decodeFromStream(getSerializer(), eventData.inputStream())
    }

    @OptIn(ExperimentalSerializationApi::class)
    fun encode(event: T, outputStream: ByteArrayOutputStream) {
        json.encodeToStream(getSerializer(), event, outputStream)
    }

    abstract fun getSerializer(): KSerializer<T>

    abstract val streamNamePrefix: String

    internal val fullStreamNamePrefix by lazy { "$streamNamePrefix-" }

    fun deserializeEventWithAggregateId(event: ResolvedEvent): Pair<DtoUUID, T>? {
        val eventType = event.event.eventType
        val eventData = event.event.eventData
        val streamId = event.event.streamId
        return if (eventType in knownEventTypes) {
            UUID.fromString(streamId.removePrefix(fullStreamNamePrefix)) to decode(eventData)
        } else {
            null
        }
    }
}
