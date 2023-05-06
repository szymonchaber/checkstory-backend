package tech.checkstory.checklist.api

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
import tech.checkstory.auth.firebase.FIREBASE_AUTH
import tech.checkstory.auth.user.User
import tech.checkstory.auth.withFirebasePrincipal
import tech.checkstory.checklist.data.EventBasedChecklistRepository
import tech.checkstory.checklist.domain.command.ChecklistCommand
import tech.checkstory.checklist.domain.event.ChecklistEvent
import java.io.ByteArrayOutputStream

fun Application.configureChecklistCommandService(
    client: EventStoreDBClient,
    repository: EventBasedChecklistRepository
) {
    routing {
        authenticate(FIREBASE_AUTH) {
            post("/checklist-commands") {
                withFirebasePrincipal {
                    val commands = call.receive<List<ChecklistCommand>>()
                    handleChecklistCommands(it, client, commands, repository)
                    call.respond(HttpStatusCode.NoContent)
                }
            }
        }
    }
}

suspend fun handleChecklistCommands(
    user: User,
    client: EventStoreDBClient,
    commands: List<ChecklistCommand>,
    repository: EventBasedChecklistRepository
) {
    commands.sortedBy { it.timestamp }
        .onEach {
            require(it.userId == user.userId) {
                "command user id must equal firebase user id"
            }
        }
        .groupBy { it.checklistId }
        .forEach { (checklistId, commands) ->
            repository.getByIdOrNew(user, checklistId)
                .applyCommands(commands)
                .forEach {
                    storeChecklistEvent(client, it)
                }
        }
}


@OptIn(ExperimentalSerializationApi::class)
fun storeChecklistEvent(client: EventStoreDBClient, checklistEvent: ChecklistEvent) {
    val eventData = ByteArrayOutputStream().use {
        Json.encodeToStream(checklistEvent, it)
        EventData
            .builderAsBinary(
                checklistEvent.eventId,
                checklistEvent.eventType,
                it.toByteArray()
            ) // event type gets stored within dto - maybe not good!
            .build()
    }

    client.appendToStream("checklist-${checklistEvent.checklistId}", eventData)
        .get()
}
