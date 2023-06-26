package tech.checkstory.read

import com.eventstore.dbclient.EventStoreDBClient
import io.ktor.server.application.*
import kotlinx.coroutines.*
import org.kodein.di.DI
import org.kodein.di.instance
import tech.checkstory.data.EventMapper
import tech.checkstory.eventbus.EventBus
import tech.checkstory.read.checklist.ChecklistProjection
import tech.checkstory.read.subscription.EventStoreDBSubscriptionToAll
import tech.checkstory.read.template.TemplateProjection
import tech.checkstory.sync.NotificationSubscription

context(DI)
fun Application.configureReadSide() {
    val eventBus by instance<EventBus>()
    val client by instance<EventStoreDBClient>()
    val mappers by instance<Set<EventMapper<*>>>()
    val subscription = EventStoreDBSubscriptionToAll(client, eventBus, mappers)
    val templateProjection by instance<TemplateProjection>()
    val checklistProjection by instance<ChecklistProjection>()
    val notificationSubscription by instance<NotificationSubscription>()
    CoroutineScope(Job()).launch {
        withContext(Dispatchers.Default) {
            notificationSubscription.run()
        }
    }
    CoroutineScope(Job()).launch {
        withContext(Dispatchers.Default) {
            templateProjection.run()
        }
    }
    CoroutineScope(Job()).launch {
        withContext(Dispatchers.Default) {
            checklistProjection.run()
        }
    }
    CoroutineScope(Job()).launch {
        withContext(Dispatchers.Default) {
            subscription.subscribeToAll()
        }
    }
}
