package tech.checkstory

import com.eventstore.dbclient.EventStoreDBClient
import com.eventstore.dbclient.EventStoreDBConnectionString
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.cors.routing.*
import org.jetbrains.exposed.sql.Database
import org.kodein.di.*
import tech.checkstory.auth.firebase.Firebase
import tech.checkstory.auth.user.UserService
import tech.checkstory.auth.user.configureUserService
import tech.checkstory.checklist.configureChecklist
import tech.checkstory.checklist.data.InMemoryChecklistEventRepository
import tech.checkstory.checklist.domain.event.ChecklistEvent
import tech.checkstory.data.EventRepository
import tech.checkstory.notification.configureNotification
import tech.checkstory.notification.data.FirebaseMessagingTokenRepository
import tech.checkstory.notification.firebase.UserDataChangeNotifier
import tech.checkstory.payment.configurePayment
import tech.checkstory.payment.data.PurchaseRepository
import tech.checkstory.payment.data.google_play.SubscriptionsGoogleApi
import tech.checkstory.payment.domain.GetPaymentStateUseCase
import tech.checkstory.payment.playbilling.configurePlayBillingPubSub
import tech.checkstory.plugins.*
import tech.checkstory.template.configureTemplate
import tech.checkstory.template.data.EventSourcedTemplateRepository
import tech.checkstory.template.data.InMemoryTemplateEventRepository
import tech.checkstory.template.domain.event.TemplateEvent

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module).start(wait = true)
}

fun Application.module() = DI {
    bind<Config>() with singleton { ConfigFactory.load() }
    bindSingleton {
        Database.connect(
            getNewConnection = {
                connectToPostgres(false, instance())
            }
        )
    }
    bind<EventStoreDBClient>() with singleton {
        val connectionString = instance<Config>().getString("eventStore.connectionString")!!
        val settings = EventStoreDBConnectionString.parse(connectionString)
        EventStoreDBClient.create(settings)
    }
    bindSingleton {
        UserService(instance())
    }
    bindSingleton {
        SubscriptionsGoogleApi()
    }
    bindSingleton {
        PurchaseRepository(instance())
    }
    bind<EventRepository<TemplateEvent>>() with singleton {
//        TemplateEventRepositoryImpl(instance())
        InMemoryTemplateEventRepository()
    }
    bind<EventRepository<ChecklistEvent>>() with singleton {
//        ChecklistEventRepositoryImpl(instance())
        InMemoryChecklistEventRepository()
    }
    bindSingleton {
        EventSourcedTemplateRepository(instance())
    }
    bindSingleton {
        FirebaseMessagingTokenRepository(instance())
    }
    bindSingleton {
        log
    }
    bindSingleton {
        UserDataChangeNotifier(instance(), instance())
    }
    bindSingleton {
        GetPaymentStateUseCase(instance())
    }
}.apply { module() }

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
    configurePlayBillingPubSub()
    configureRouting()
    cors()
}

fun Application.cors() {
    install(CORS) {
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Patch)
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.AccessControlAllowOrigin)
        allowHeader(HttpHeaders.ContentType)
        allowNonSimpleContentTypes = true
        allowCredentials = true
        allowSameOrigin = true
        allowHost("*", listOf("http", "https")) // TODO frontendHost might be "*" but should not be
    }
}
