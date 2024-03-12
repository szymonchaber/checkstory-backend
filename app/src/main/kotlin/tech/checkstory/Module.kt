package tech.checkstory

import io.ktor.server.application.*
import io.sentry.Sentry
import kotlinx.coroutines.*
import org.kodein.di.DI
import org.kodein.di.instance
import tech.checkstory.auth.firebase.Firebase
import tech.checkstory.auth.module.configureAuth
import tech.checkstory.checklist.configureChecklist
import tech.checkstory.command.NotificationSubscription
import tech.checkstory.command.configureSynchronization
import tech.checkstory.config.cors
import tech.checkstory.data.EventMapper
import tech.checkstory.data.EventStoreConnectionManager
import tech.checkstory.eventbus.EventBus
import tech.checkstory.notification.configureNotification
import tech.checkstory.payment.configurePayment
import tech.checkstory.payment.playbilling.configurePlayBillingPubSub
import tech.checkstory.plugins.*
import tech.checkstory.read.subscription.SubscriptionCheckpointRepository
import tech.checkstory.read.subscription.SubscriptionToAll
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
    configureAuth()
    configureNotification()
    configureTemplate()
    configureChecklist()
    configureSynchronization()
    configurePlayBillingPubSub()
    configureRouting()
    configureReadSide()
    cors()
    environment.monitor.subscribe(ApplicationStopping) {
        Sentry.close()
    }
}

context(DI)
private fun Application.configureReadSide() {
    val eventBus by instance<EventBus>()
    val connectionManager by instance<EventStoreConnectionManager>()
    val mappers by instance<Set<EventMapper<*>>>()
    val subscriptionCheckpointRepository by instance<SubscriptionCheckpointRepository>()
    val subscription by instance<SubscriptionToAll>()
    val templateProjection by instance<TemplateProjection>()
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
            subscription.subscribeToAll()
        }
    }
}
