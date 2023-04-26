package tech.checkstory.template

import com.eventstore.dbclient.EventData
import com.eventstore.dbclient.EventStoreDBClient
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import tech.checkstory.auth.withFirebasePrincipal
import tech.checkstory.firebase.FIREBASE_AUTH
import tech.checkstory.firebase.user.FirebaseUser
import java.util.*

fun Application.configureEventService(client: EventStoreDBClient) {
    routing {
        authenticate(FIREBASE_AUTH) {
            post("/commands") {
                withFirebasePrincipal {
                    val commands = call.receive<List<DomainCommand>>()
                    handleCommands(it, client, commands)
                    call.respond(HttpStatusCode.NoContent)
                }
            }
        }
    }
}

suspend fun handleCommands(
    firebaseUser: FirebaseUser,
    client: EventStoreDBClient,
    commands: List<DomainCommand>
) {
    commands
        .sortedBy { it.timestamp }
        .forEach {
            // TODO also check if the userId in the existing stream is the same as requester's
            require(it.userId == firebaseUser.userId)
            when (it) {
                is TemplateCommand -> {
                    storeTemplateEvent(client, it.toTemplateEvent())
                }
            }
        }
}

fun storeTemplateEvent(client: EventStoreDBClient, templateEvent: TemplateEvent) {
    val eventData = EventData
        .builderAsJson<Any>(
            UUID.fromString(templateEvent.eventId),
            templateEvent.eventType,
            templateEvent
        ) // event type gets stored within dto - maybe not good!
        .build()
    client.appendToStream("checklist-template-${templateEvent.templateId}", eventData)
        .get()
}
