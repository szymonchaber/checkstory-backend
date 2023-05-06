package tech.checkstory

import com.eventstore.dbclient.EventStoreDBClient
import com.eventstore.dbclient.EventStoreDBConnectionString
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
