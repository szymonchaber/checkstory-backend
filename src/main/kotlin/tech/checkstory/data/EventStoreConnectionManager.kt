package tech.checkstory.data

import com.eventstore.dbclient.EventStoreDBClient
import com.eventstore.dbclient.EventStoreDBConnectionString
import org.kodein.di.Volatile
import java.net.URLEncoder

class EventStoreConnectionManager(eventStorePassword: String) {

    private val connectionString by lazy {
        val escapedPassword = URLEncoder.encode(eventStorePassword, "UTF-8")
        "esdb://admin:${escapedPassword}" +
                "@esdb-node-1:2113,esdb-node-2:2113,esdb-node-3:2113" +
                "?tls=true&keepAliveTimeout=10000&keepAliveInterval=10000&tlsVerifyCert=false"
    }

    @Volatile
    private var client: EventStoreDBClient? = null

    fun getClient(): EventStoreDBClient {
        val tempClient = client
        if (tempClient == null || tempClient.isShutdown) {
            synchronized(this) {
                if (client == null || client?.isShutdown == true) {
                    val settings = EventStoreDBConnectionString.parse(connectionString)
                    client = EventStoreDBClient.create(settings)
                }
            }
        }
        return client!!
    }
}
