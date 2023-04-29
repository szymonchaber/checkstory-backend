package tech.checkstory.template

import com.eventstore.dbclient.EventData
import com.eventstore.dbclient.EventStoreDBClient
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToStream
import tech.checkstory.auth.withFirebasePrincipal
import tech.checkstory.checklist.ChecklistCommand
import tech.checkstory.checklist.ChecklistEvent
import tech.checkstory.checklist.toChecklistEvent
import tech.checkstory.firebase.FIREBASE_AUTH
import tech.checkstory.firebase.user.FirebaseUser
import java.io.ByteArrayOutputStream
import java.util.*

fun Application.configureChecklistCommandService(client: EventStoreDBClient) {
    routing {
        authenticate(FIREBASE_AUTH) {
            post("/checklist-commands") {
                withFirebasePrincipal {
                    val commands = call.receive<List<ChecklistCommand>>()
                    handleChecklistCommands(it, client, commands)
                    call.respond(HttpStatusCode.NoContent)
                }
            }
        }
    }
}

suspend fun handleChecklistCommands(
    firebaseUser: FirebaseUser,
    client: EventStoreDBClient,
    commands: List<ChecklistCommand>
) {
    commands
        .sortedBy { it.timestamp }
        .forEach {
            // TODO also check if the userId in the existing stream is the same as requester's
            require(it.userId == firebaseUser.userId) {
                "event user id must equal firebase user id"
            }
            storeChecklistEvent(client, it.toChecklistEvent())
        }
}

@OptIn(ExperimentalSerializationApi::class)
fun storeChecklistEvent(client: EventStoreDBClient, checklistEvent: ChecklistEvent) {
    val eventData = ByteArrayOutputStream().use {
        Json.encodeToStream(checklistEvent, it)
        EventData
            .builderAsBinary(
                UUID.fromString(checklistEvent.eventId),
                checklistEvent.eventType,
                it.toByteArray()
            ) // event type gets stored within dto - maybe not good!
            .build()
    }

    client.appendToStream("checklist-${checklistEvent.checklistId}", eventData)
        .get()
}
