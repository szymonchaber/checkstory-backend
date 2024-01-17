package tech.checkstory

import io.ktor.server.application.*
import kotlinx.coroutines.*
import org.kodein.di.DI
import org.kodein.di.instance
import tech.checkstory.auth.firebase.Firebase
import tech.checkstory.auth.user.configureUserService
import tech.checkstory.checklist.configureChecklist
import tech.checkstory.config.cors
import tech.checkstory.data.EventMapper
import tech.checkstory.data.EventStoreConnectionManager
import tech.checkstory.eventbus.EventBus
import tech.checkstory.notification.configureNotification
import tech.checkstory.payment.configurePayment
import tech.checkstory.payment.playbilling.configurePlayBillingPubSub
import tech.checkstory.plugins.*
import tech.checkstory.read.checklist.ChecklistProjection
import tech.checkstory.read.subscription.SubscriptionCheckpointRepository
import tech.checkstory.read.subscription.SubscriptionToAll
import tech.checkstory.sync.NotificationSubscription
import tech.checkstory.sync.configureSynchronization
import tech.checkstory.template.data.TemplateProjection
import tech.checkstory.template.module.configureTemplate

context(DI)
fun Application.module() {
    Firebase.init()
    configureHeaders()
    configureSecurity()
    configurePayment()
    configureHTTP()
    configureMonitoring()
    configureSerialization()
    configureUserService()
    configureNotification()
    configureTemplate()
    configureChecklist()
    configureSynchronization()
    configurePlayBillingPubSub()
    configureRouting()
    configureReadSide()
    cors()
}

context(DI)
private fun Application.configureReadSide() {
    val eventBus by instance<EventBus>()
    val connectionManager by instance<EventStoreConnectionManager>()
    val mappers by instance<Set<EventMapper<*>>>()
    val subscriptionCheckpointRepository by instance<SubscriptionCheckpointRepository>()
    val subscription by instance<SubscriptionToAll>()
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
