package tech.checkstory.api

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
import tech.checkstory.domain.command.DomainCommand
import tech.checkstory.template.domain.command.TemplateCommand
import tech.checkstory.template.domain.command.toTemplateEvent
import tech.checkstory.template.domain.event.TemplateEvent
import java.io.ByteArrayOutputStream

fun Application.configureTemplateCommandService(client: EventStoreDBClient) {
    routing {
        authenticate(FIREBASE_AUTH) {
            post("/commands") {
                withFirebasePrincipal {
                    val commands = call.receive<List<TemplateCommand>>()
                    handleCommands(it, client, commands)
                    call.respond(HttpStatusCode.NoContent)
                }
            }
        }
    }
}

suspend fun handleCommands(
    user: User,
    client: EventStoreDBClient,
    commands: List<DomainCommand>
) {
    commands
        .sortedBy { it.timestamp }
        .forEach {
            // TODO also check if the userId in the existing stream is the same as requester's
            require(it.userId == user.userId)
            when (it) {
                is TemplateCommand -> {
                    storeTemplateEvent(client, it.toTemplateEvent())
                }
            }
        }
}

@OptIn(ExperimentalSerializationApi::class)
fun storeTemplateEvent(client: EventStoreDBClient, templateEvent: TemplateEvent) {
    val eventData = ByteArrayOutputStream().use {
        Json.encodeToStream(templateEvent, it)
        EventData
            .builderAsBinary(
                templateEvent.eventId,
                templateEvent.eventType,
                it.toByteArray()
            ) // event type gets stored within dto - maybe not good!
            .build()
    }

    client.appendToStream("checklist-template-${templateEvent.templateId}", eventData)
        .get()
}
