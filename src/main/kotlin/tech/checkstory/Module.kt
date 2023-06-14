package tech.checkstory

import io.ktor.server.application.*
import org.kodein.di.DI
import tech.checkstory.auth.firebase.Firebase
import tech.checkstory.auth.user.configureUserService
import tech.checkstory.checklist.configureChecklist
import tech.checkstory.config.cors
import tech.checkstory.notification.configureNotification
import tech.checkstory.payment.configurePayment
import tech.checkstory.payment.playbilling.configurePlayBillingPubSub
import tech.checkstory.plugins.*
import tech.checkstory.read.configureReadSide
import tech.checkstory.sync.configureSynchronization
import tech.checkstory.template.configureTemplate

context(DI)
fun Application.module() {
    Firebase.init()
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
