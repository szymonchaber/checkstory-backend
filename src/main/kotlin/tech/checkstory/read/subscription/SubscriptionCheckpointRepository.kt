package tech.checkstory.read.subscription

import com.eventstore.dbclient.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import tech.checkstory.data.EventStoreConnectionManager
import java.io.ByteArrayOutputStream

@OptIn(ExperimentalSerializationApi::class)
class SubscriptionCheckpointRepository(
    private val connectionManager: EventStoreConnectionManager,
    private val json: Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }
) {

    private var EVENT_TYPE: String = "CheckpointStored"

    suspend fun load(subscriptionId: String): Long? {
        return withContext(Dispatchers.IO) {
            try {
                val options = ReadStreamOptions.get()
                    .backwards()
                    .fromEnd()
                connectionManager.getClient()
                    .readStream(
                        getCheckpointStreamName(subscriptionId),
                        options
                    )
                    .await()
                    .events
                    .firstOrNull()
                    ?.let(::deserializeEvent)
                    ?.position
            } catch (exception: StreamNotFoundException) {
                null
            }
        }
    }

    suspend fun store(subscriptionId: String, position: Long) {
        return withContext(Dispatchers.IO) {
            val eventData =
                toEventData(
                    CheckpointStored(
                        subscriptionId,
                        position,
                        Clock.System.now().toLocalDateTime(TimeZone.UTC)
                    )
                )
            val eventStore = connectionManager.getClient()

            connectionManager.getClient()
                .appendToStream(
                    getCheckpointStreamName(subscriptionId), eventData
                )
                .await()
            val streamName: String = getCheckpointStreamName(subscriptionId)
            try {
                // store new checkpoint expecting stream to exist
                eventStore.appendToStream(
                    streamName,
                    AppendToStreamOptions.get().expectedRevision(ExpectedRevision.streamExists()),
                    eventData
                ).get()
            } catch (throwable: Throwable) {
                if (throwable.cause !is WrongExpectedVersionException) throw RuntimeException(throwable)

                // WrongExpectedVersionException means that stream did not exist
                // Set the checkpoint stream to have at most 1 event
                // using stream metadata $maxCount property
                val keepOnlyLastEvent = StreamMetadata()
                keepOnlyLastEvent.maxCount = 1
                try {
                    eventStore.setStreamMetadata(
                        streamName,
                        AppendToStreamOptions.get().expectedRevision(ExpectedRevision.noStream()),
                        keepOnlyLastEvent
                    ).get()

                    eventStore.appendToStream(
                        streamName,
                        AppendToStreamOptions.get().expectedRevision(ExpectedRevision.noStream()),
                        eventData
                    ).get()
                } catch (exception: Exception) {
                    throw RuntimeException(throwable)
                }
            }
        }
    }

    private fun getCheckpointStreamName(subscriptionId: String): String {
        return "checkpoint_$subscriptionId"
    }

    private fun toEventData(event: CheckpointStored): EventData {
        val eventDataBinary = ByteArrayOutputStream().use { outputArray ->
            encode(event, outputArray)
            outputArray.toByteArray()
        }
        return EventData.builderAsBinary(EVENT_TYPE, eventDataBinary).build()
    }

    private fun deserializeEvent(event: ResolvedEvent): CheckpointStored? {
        val eventType = event.event.eventType
        val eventData = event.event.eventData
        return if (eventType == EVENT_TYPE) {
            decode(eventData)
        } else {
            null
        }
    }

    private fun decode(eventData: ByteArray): CheckpointStored {
        return json.decodeFromStream(CheckpointStored.serializer(), eventData.inputStream())
    }

    private fun encode(event: CheckpointStored, outputStream: ByteArrayOutputStream) {
        json.encodeToStream(CheckpointStored.serializer(), event, outputStream)
    }
}

@Serializable
data class CheckpointStored(val subscriptionId: String, val position: Long, val checkpointedAt: LocalDateTime)
