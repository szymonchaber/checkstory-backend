package tech.checkstory

import com.eventstore.dbclient.EventStoreDBClient
import com.eventstore.dbclient.EventStoreDBConnectionString
import com.google.cloud.pubsub.v1.AckReplyConsumer
import com.google.cloud.pubsub.v1.MessageReceiver
import com.google.cloud.pubsub.v1.Subscriber
import com.google.pubsub.v1.ProjectSubscriptionName
import com.google.pubsub.v1.PubsubMessage
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.cors.routing.*
import org.jetbrains.exposed.sql.Database
import tech.checkstory.auth.firebase.FirebaseAdmin
import tech.checkstory.auth.user.UserService
import tech.checkstory.auth.user.configureUserService
import tech.checkstory.checklist.configureChecklist
import tech.checkstory.checklist.data.ChecklistEventRepository
import tech.checkstory.plugins.*
import tech.checkstory.template.configureTemplate
import java.sql.Connection
import java.util.concurrent.TimeoutException


fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module, configure = {
        FirebaseAdmin.init()
    })
        .start(wait = true)
}

fun Application.module() {
    val database = Database.connect(
        url = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1",
        user = "root",
        driver = "org.h2.Driver",
        password = ""
    )
    val connection: Connection = connectToPostgres(embedded = true)
    val userService = UserService(database)
    configureSecurity(userService)
    configureHTTP()
    configureMonitoring()
    configureSerialization()
    configureUserService(userService)
    val settings =
        EventStoreDBConnectionString.parse("esdb+discover://localhost:2113?tls=false&keepAliveTimeout=10000&keepAliveInterval=10000")
    val client = EventStoreDBClient.create(settings)
    configureTemplate(database, client)
    configureChecklist(ChecklistEventRepository(client))
    configureRouting()
    cors()
}

fun Application.cors() {
    install(CORS) {
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Patch)
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.AccessControlAllowOrigin)
        allowHeader(HttpHeaders.ContentType)
        allowNonSimpleContentTypes = true
        allowCredentials = true
        allowSameOrigin = true
        allowHost("*", listOf("http", "https")) // TODO frontendHost might be "*" but should not be
        this@cors.log.info("Enabled cors!")
    }
}


object SubscribeAsyncExample {
    @Throws(Exception::class)
    @JvmStatic
    fun main(args: Array<String>) {
        // TODO(developer): Replace these variables before running the sample.
        subscribeAsyncExample(projectId, subscriptionId)
    }

    fun subscribeAsyncExample(projectId: String?, subscriptionId: String?) {
        val subscriptionName = ProjectSubscriptionName.of(projectId, subscriptionId)

        // Instantiate an asynchronous message receiver.
        val receiver = MessageReceiver { message: PubsubMessage, consumer: AckReplyConsumer ->
            // Handle incoming message, then ack the received message.
            println("Id: " + message.messageId)
            println("Data: " + message.data.toStringUtf8())
            try {
                handlePaymentNotification(message)
                consumer.ack()
            } catch (exception: Exception) {
                consumer.nack()

            }
        }
        var subscriber: Subscriber? = null
        try {
            subscriber = Subscriber.newBuilder(subscriptionName, receiver).build()
            // Start the subscriber.
            subscriber.startAsync().awaitRunning()
            System.out.printf("Listening for messages on %s:\n", subscriptionName.toString())
            // Allow the subscriber to run for 30s unless an unrecoverable error occurs.
//            subscriber.awaitTerminated(30, TimeUnit.SECONDS)
            readLine()
        } catch (timeoutException: TimeoutException) {
            // Shut down the subscriber after 30s. Stop receiving messages.
            subscriber!!.stopAsync()
        }
    }


    fun runSubscriber(projectId: String, subId: String, receiver: MessageReceiver) {
        val subName = ProjectSubscriptionName.of(projectId, subId)
        val sub = Subscriber.newBuilder(subName, receiver).build()
        try {
            sub.startAsync().awaitRunning()
            println("Listening for messages on $subName. Press ENTER to quit.")
            readLine()
        } catch (e: Exception) {
            println("Stopped due to exception: ${e.message}, ${e.stackTraceToString()}")
            sub.stopAsync()
        }
    }

    private fun handlePaymentNotification(message: PubsubMessage) {
    }
}