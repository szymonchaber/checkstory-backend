package tech.checkstory

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
    val templateService = TemplateService(database)
    configureTemplateService(
        templateService
    )
    configureEventService(templateService)
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
