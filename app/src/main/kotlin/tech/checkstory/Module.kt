package tech.checkstory

import io.ktor.server.application.*
import io.sentry.Sentry
import org.kodein.di.DI
import tech.checkstory.auth.module.configureAuth
import tech.checkstory.checklist.configureChecklist
import tech.checkstory.config.cors
import tech.checkstory.notification.configureNotification
import tech.checkstory.payment.configurePayment
import tech.checkstory.payment.playbilling.configurePlayBillingPubSub
import tech.checkstory.plugins.*
import tech.checkstory.template.module.configureSynchronization
import tech.checkstory.template.module.configureTemplate

context(DI)
fun Application.module() {
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
    cors()
    environment.monitor.subscribe(ApplicationStopping) {
        Sentry.close()
    }
}
