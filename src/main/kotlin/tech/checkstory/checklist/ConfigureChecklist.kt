package tech.checkstory.checklist

import com.eventstore.dbclient.EventStoreDBClient
import io.ktor.server.application.*
import tech.checkstory.checklist.api.configureChecklistCommandService
import tech.checkstory.checklist.api.configureChecklistService
import tech.checkstory.checklist.data.EventBasedChecklistRepository

fun Application.configureChecklist(client: EventStoreDBClient) {
    val checklistRepository = EventBasedChecklistRepository(client)
    configureChecklistService(checklistRepository)
    configureChecklistCommandService(client, checklistRepository)
}