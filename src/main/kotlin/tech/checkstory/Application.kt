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
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.Database
import org.kodein.di.*
import tech.checkstory.auth.firebase.Firebase
import tech.checkstory.auth.user.UserService
import tech.checkstory.auth.user.configureUserService
import tech.checkstory.checklist.configureChecklist
import tech.checkstory.checklist.data.ChecklistEventRepository
import tech.checkstory.checklist.data.ChecklistEventRepositoryImpl
import tech.checkstory.notification.configureNotification
import tech.checkstory.notification.data.FirebaseMessagingTokenRepository
import tech.checkstory.notification.firebase.UserDataChangeNotifier
import tech.checkstory.payment.configurePayment
import tech.checkstory.payment.data.PurchaseRepository
import tech.checkstory.payment.data.google_play.SubscriptionsGoogleApi
import tech.checkstory.payment.domain.GetPaymentStateUseCase
import tech.checkstory.payment.playbilling.PlayBillingPubSubHandler
import tech.checkstory.plugins.*
import tech.checkstory.template.configureTemplate
import tech.checkstory.template.data.EventSourcedTemplateRepository
import tech.checkstory.template.data.TemplateEventRepository
import java.util.*

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module).start(wait = true)
}

fun Application.module() = module(DI {
    bind<Config>() with singleton { ConfigFactory.load() }
    bind<EventStoreDBClient>() with singleton {
        val connectionString = instance<Config>().getString("eventStore.connectionString")!!
        val settings = EventStoreDBConnectionString.parse(connectionString)
        EventStoreDBClient.create(settings)
    }
    bind<TemplateEventRepository>() with singleton {
        TemplateEventRepository(instance())
    }
    bind<ChecklistEventRepository>() with singleton {
        ChecklistEventRepositoryImpl(instance())
    }
})

/**
 * This is the application module that has a
 * preconfigured [kodein] instance as input.
 *
 * The idea of this method, is that the different modules
 * can call this with several configured kodein variants
 * and also you can call it from the tests setting mocks
 * instead of the default mappings.
 */
private operator fun Random.get(range: IntRange) = range.first + this.nextInt(range.last - range.first)
fun Application.module(kodein: DI) {
    val config by kodein.instance<Config>()
    val database = Database.connect(getNewConnection = {
        connectToPostgres(false, config)
    })
    Firebase.init()
    val userService = UserService(database)
    val subscriptionsGoogleApi = SubscriptionsGoogleApi()
    val purchaseRepository = PurchaseRepository(database)
    configureSecurity(userService, GetPaymentStateUseCase(purchaseRepository))
    configurePayment(purchaseRepository, subscriptionsGoogleApi)
    configureHTTP()
    configureMonitoring()
    configureSerialization()
    configureUserService(userService)
    val messagingTokenRepository = FirebaseMessagingTokenRepository(database)
    configureNotification(messagingTokenRepository)
    val client by kodein.instance<EventStoreDBClient>()
    val templateEventRepository by kodein.instance<TemplateEventRepository>()
    val eventSourcedTemplateRepository = EventSourcedTemplateRepository(templateEventRepository)
    val userDataChangeNotifier = UserDataChangeNotifier(messagingTokenRepository, log)
    configureTemplate(
        database,
        TemplateEventRepository(client),
        eventSourcedTemplateRepository,
        userDataChangeNotifier
    )
    val checklistEventRepository by kodein.instance<ChecklistEventRepository>()
    configureChecklist(checklistEventRepository, eventSourcedTemplateRepository, userDataChangeNotifier)
    PlayBillingPubSubHandler(subscriptionsGoogleApi, purchaseRepository, userDataChangeNotifier)
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
