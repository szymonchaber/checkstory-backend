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
import tech.checkstory.auth.withFirebasePrincipal
import tech.checkstory.checklist.data.EventBasedChecklistRepository
import tech.checkstory.checklist.domain.aggregate.ChecklistAggregate
import tech.checkstory.checklist.domain.command.ChecklistCommand
import tech.checkstory.checklist.domain.event.ChecklistEvent
import tech.checkstory.domain.command.DtoUUID
import tech.checkstory.firebase.FIREBASE_AUTH
import tech.checkstory.firebase.user.FirebaseUser
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
    firebaseUser: FirebaseUser,
    client: EventStoreDBClient,
    commands: List<ChecklistCommand>,
    repository: EventBasedChecklistRepository
) {
    commands.sortedBy { it.timestamp }
        .onEach {
            require(it.userId == firebaseUser.userId) {
                "command user id must equal firebase user id"
            }
        }
        .groupBy { it.checklistId }
        .forEach { (checklistId, commands) ->
            getAggregate(repository, firebaseUser, checklistId, commands)?.let { (aggregate, createEvent) ->
                createEvent?.let { storeChecklistEvent(client, it) }
                aggregate
                    .applyCommands(commands.filterNot { it is ChecklistCommand.CreateChecklistCommand })
                    .forEach {
                        storeChecklistEvent(client, it)
                    }
            }
        }
}

private suspend fun getAggregate(
    repository: EventBasedChecklistRepository,
    firebaseUser: FirebaseUser,
    checklistId: DtoUUID,
    commands: List<ChecklistCommand>
): Pair<ChecklistAggregate, ChecklistEvent.CreateChecklistEvent?>? {
    return repository.getAggregate(firebaseUser, checklistId)?.let { it to null } ?: aggregateOrNull(commands)
}

fun aggregateOrNull(commands: List<ChecklistCommand>): Pair<ChecklistAggregate, ChecklistEvent.CreateChecklistEvent>? {
    return when (val createEvent = commands.firstOrNull()) {
        is ChecklistCommand.CreateChecklistCommand -> {
            ChecklistAggregate(createEvent) to ChecklistEvent.CreateChecklistEvent(
                checklistId = createEvent.checklistId,
                templateId = createEvent.templateId,
                title = createEvent.title,
                description = createEvent.description,
                tasks = createEvent.tasks,
                eventId = createEvent.commandId,
                timestamp = createEvent.timestamp,
                userId = createEvent.userId
            )
        }

        else -> null
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
