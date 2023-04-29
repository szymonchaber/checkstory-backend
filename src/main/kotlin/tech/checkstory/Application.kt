package tech.checkstory

import com.eventstore.dbclient.EventStoreDBClient
import com.eventstore.dbclient.EventStoreDBClientSettings
import com.eventstore.dbclient.EventStoreDBConnectionString
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.cors.routing.*
import org.jetbrains.exposed.sql.Database
import tech.checkstory.checklist.EventBasedChecklistRepository
import tech.checkstory.city.configureCityService
import tech.checkstory.firebase.FirebaseAdmin
import tech.checkstory.plugins.*
import tech.checkstory.template.*
import tech.checkstory.template.task.TemplateTaskService
import tech.checkstory.user.configureUserService


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
    val templateService = RelationalTemplateRepository(database)
    val templateTaskService = TemplateTaskService(database)
    configureTemplateService(templateService, EventBasedTemplateRepository(client))
    configureChecklistService(EventBasedChecklistRepository(client))
    configureTemplateTaskService(templateService, templateTaskService)
    configureTemplateCommandService(client)
    configureChecklistCommandService(client)
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
