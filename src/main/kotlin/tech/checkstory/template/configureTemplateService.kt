package tech.checkstory.template

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.jetbrains.exposed.sql.Database
import tech.checkstory.auth.withFirebasePrincipal
import tech.checkstory.firebase.FIREBASE_AUTH
import tech.checkstory.firebase.user.FirebaseUser
import java.util.*

fun Application.configureTemplateService() {

    val database = Database.connect(
        url = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1",
        user = "root",
        driver = "org.h2.Driver",
        password = ""
    )
    val templateService = TemplateService(database)
    routing {
        authenticate(FIREBASE_AUTH) {

            // Create user
            post("/templates") {
                withFirebasePrincipal {
                    val user = call.receive<Template>()
                    val id = templateService.create(user, it)
                    call.respond(HttpStatusCode.Created, id)
                }
            }

            // Read user
            get("/templates") {
                withFirebasePrincipal {
                    val templates = templateService.read(it)
                    call.respond(HttpStatusCode.OK, templates)
                }
            }
            // Read user
            get("/hello") {
                withFirebasePrincipal {
                    call.respond(HttpStatusCode.OK, "Principal has mail: ${it.email} so we allow them to come!")
                }
            }

            // Read user
            get("/templates/{id}") {
                withFirebasePrincipal {
                    val id = call.parameters["id"] ?: throw IllegalArgumentException("Invalid ID")
                    val template = templateService.read(it, UUID.fromString(id))
                    if (template != null) {
                        call.respond(HttpStatusCode.OK, template)
                    } else {
                        call.respond(HttpStatusCode.NotFound)
                    }
                }
            }

            // Update user
//            put("/user") {
//                withFirebasePrincipal {
//                    val user = call.receive<Template>()
//                    templateService.update(it.userId, user)
//                    call.respond(HttpStatusCode.OK)
//                }
//            }

            // Delete user
            delete("/templates/{id}") {
                withFirebasePrincipal {
                    val id = call.parameters["id"] ?: throw IllegalArgumentException("Invalid ID")
                    templateService.delete(it, UUID.fromString(id))
                    call.respond(HttpStatusCode.OK)
                }
            }

            post("/events") {
                withFirebasePrincipal {
                    val domainEventDto = call.receive<List<DomainEventDto>>()
                    println("Received: $domainEventDto")
                    updateStateBasedOnEvents(it, templateService, domainEventDto)
                    call.respond(HttpStatusCode.Created)
                }
            }
        }
    }
}

suspend fun updateStateBasedOnEvents(
    firebaseUser: FirebaseUser,
    service: TemplateService,
    events: List<DomainEventDto>
) {
    events
        .sortedBy { it.timestamp }
        .forEach {
            when (it.eventType) {
                "createTemplate" -> {
                    val data = Json.decodeFromJsonElement(CreateTemplateEventData.serializer(), it.data)
                    service.create(UUID.fromString(data.id), Template("", ""), firebaseUser)
                }

                "editTemplateTitle" -> {
                    val data = Json.decodeFromJsonElement(EditTemplateTitleEventData.serializer(), it.data)
                    service.updateTitle(firebaseUser, UUID.fromString(data.id), data.newTitle)
                }

                else -> Unit
            }
        }
}

@Serializable
data class DomainEventDto(val eventType: String, val timestamp: Long, val data: JsonObject)

@Serializable
data class CreateTemplateEventData(val id: String)

@Serializable
data class EditTemplateTitleEventData(val id: String, val newTitle: String)
