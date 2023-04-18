package tech.checkstory

import com.eventstore.dbclient.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.Database
import tech.checkstory.city.configureCityService
import tech.checkstory.firebase.FIREBASE_AUTH
import tech.checkstory.firebase.FirebaseAdmin
import tech.checkstory.firebase.user.FirebaseUser
import tech.checkstory.plugins.*
import tech.checkstory.template.TemplateService
import tech.checkstory.template.configureEventService
import tech.checkstory.template.configureTemplateService
import tech.checkstory.template.configureTemplateTaskService
import tech.checkstory.template.task.TemplateTaskService
import tech.checkstory.user.configureUserService
import java.util.*


fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module, configure = {
        FirebaseAdmin.init()
    })
        .start(wait = true)
}

fun Application.module() {
    configureSecurity()
    configureHTTP()
    configureMonitoring()
    configureSerialization()
    configureUserService()
    configureCityService()
    val database = Database.connect(
        url = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1",
        user = "root",
        driver = "org.h2.Driver",
        password = ""
    )
    val settings: EventStoreDBClientSettings =
        EventStoreDBConnectionString.parse("esdb+discover://localhost:2113?tls=false&keepAliveTimeout=10000&keepAliveInterval=10000")
    val client: EventStoreDBClient = EventStoreDBClient.create(settings)
    configureEventStore(client)
    val templateService = TemplateService(database)
    val templateTaskService = TemplateTaskService(database)
    configureTemplateService(templateService)
    configureTemplateTaskService(templateService, templateTaskService)
    configureEventService(templateService, templateTaskService)
    configureRouting()
    routing {
        authenticate(FIREBASE_AUTH) {
            get("/authenticated") {
                val user: FirebaseUser = call.principal() ?: return@get call.respond(HttpStatusCode.Unauthorized)
                call.respond("User is authenticated, my successful person!: $user")
            }
        }
    }
    cors()
}

fun configureEventStore(client: EventStoreDBClient) {
    val event = TestEvent(
        id = UUID.randomUUID().toString(),
        importantData = "I wrote my first event!"
    )

    val eventData = EventData
        .builderAsJson<Any>("TestEvent", event)
        .build()
    client.appendToStream("some-stream", eventData)
        .get()

    val options: ReadStreamOptions = ReadStreamOptions.get()
        .forwards()
        .fromStart()
        .maxCount(10)

    val result: ReadResult = client.readStream("some-stream", options)
        .get()
    println(result.events.joinToString {
        it.event.eventType + ":" + it.event.eventData
    })
}

data class TestEvent(val id: String, val importantData: String)

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
