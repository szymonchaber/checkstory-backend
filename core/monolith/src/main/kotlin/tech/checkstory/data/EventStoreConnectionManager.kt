package tech.checkstory.data

import com.eventstore.dbclient.EventStoreDBClient
import com.eventstore.dbclient.EventStoreDBConnectionString
import org.kodein.di.Volatile

class EventStoreConnectionManager(eventStoreConnectionString: String) {

    private val settings = EventStoreDBConnectionString.parseOrThrow(eventStoreConnectionString)

    @Volatile
    private var client: EventStoreDBClient? = null

    fun getClient(): EventStoreDBClient {
        val tempClient = client
        if (tempClient == null || tempClient.isShutdown) {
            synchronized(this) {
                if (client == null || client?.isShutdown == true) {
                    client = EventStoreDBClient.create(settings)
                }
            }
        }
        return client!!
    }
}
