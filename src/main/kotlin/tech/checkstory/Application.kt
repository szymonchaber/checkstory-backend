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
import tech.checkstory.api.configureTemplateCommandService
import tech.checkstory.auth.firebase.FirebaseAdmin
import tech.checkstory.auth.user.UserService
import tech.checkstory.auth.user.configureUserService
import tech.checkstory.checklist.api.configureChecklistCommandService
import tech.checkstory.checklist.api.configureChecklistService
import tech.checkstory.checklist.data.EventBasedChecklistRepository
import tech.checkstory.city.configureCityService
import tech.checkstory.plugins.*
import tech.checkstory.template.api.configureTemplateService
import tech.checkstory.template.api.configureTemplateTaskService
import tech.checkstory.template.data.EventBasedTemplateRepository
import tech.checkstory.template.data.RelationalTemplateRepository
import tech.checkstory.template.data.TemplateTaskRepository


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
    val userService = UserService(database)
    configureSecurity(userService)
    configureHTTP()
    configureMonitoring()
    configureSerialization()
    configureUserService(userService)
    configureCityService()
    val settings: EventStoreDBClientSettings =
        EventStoreDBConnectionString.parse("esdb+discover://localhost:2113?tls=false&keepAliveTimeout=10000&keepAliveInterval=10000")
    val client: EventStoreDBClient = EventStoreDBClient.create(settings)
    val templateService = RelationalTemplateRepository(database)
    val templateTaskRepository = TemplateTaskRepository(database)
    configureTemplateService(templateService, EventBasedTemplateRepository(client))
    val checklistRepository = EventBasedChecklistRepository(client)
    configureChecklistService(checklistRepository)
    configureTemplateTaskService(templateService, templateTaskRepository)
    configureTemplateCommandService(client)
    configureChecklistCommandService(client, checklistRepository)
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
