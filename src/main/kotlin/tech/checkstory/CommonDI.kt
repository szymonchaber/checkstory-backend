package tech.checkstory

import com.eventstore.dbclient.EventStoreDBClient
import com.eventstore.dbclient.EventStoreDBConnectionString
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.ktor.server.application.*
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.Database
import org.kodein.di.*
import tech.checkstory.auth.user.UserService
import tech.checkstory.checklist.data.ChecklistEventMapper
import tech.checkstory.checklist.data.ChecklistEventRepositoryImpl
import tech.checkstory.checklist.domain.event.ChecklistEvent
import tech.checkstory.data.EventMapper
import tech.checkstory.data.EventRepository
import tech.checkstory.eventbus.EventBus
import tech.checkstory.notification.data.FirebaseMessagingTokenRepository
import tech.checkstory.notification.firebase.UserDataChangeNotifier
import tech.checkstory.payment.data.PurchaseRepository
import tech.checkstory.payment.data.google_play.SubscriptionsGoogleApi
import tech.checkstory.payment.domain.GetPaymentStateUseCase
import tech.checkstory.plugins.connectToPostgres
import tech.checkstory.read.template.TemplateProjection
import tech.checkstory.template.data.EventSourcedTemplateRepository
import tech.checkstory.template.data.RelationalTemplateRepository
import tech.checkstory.template.data.TemplateEventMapper
import tech.checkstory.template.data.TemplateEventRepositoryImpl
import tech.checkstory.template.domain.event.TemplateEvent

val Application.commonDI
    get() = DI.Module("common") {
        bind<Config>() with singleton { ConfigFactory.load() }
        bindSingleton<Database> {
            Database.connect(
                getNewConnection = {
                    connectToPostgres(false, instance())
                }
            )
        }
        bindSingleton {
            Json {
                encodeDefaults = true
                explicitNulls = false
                prettyPrint = true
            }
        }
        bindSingleton {
            EventBus()
        }
        bind<EventStoreDBClient>() with singleton {
            val connectionString = instance<Config>().getString("eventStore.connectionString")!!
            val settings = EventStoreDBConnectionString.parse(connectionString)
            EventStoreDBClient.create(settings)
        }
        bindSingleton {
            RelationalTemplateRepository(instance())
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
        bindSingleton {
            ChecklistEventMapper()
        }
        bindSingleton {
            TemplateEventMapper()
        }
        bindSet<EventMapper<*>> {
            add {
                singleton { instance<ChecklistEventMapper>() }
            }
            add {
                singleton { instance<TemplateEventMapper>() }
            }
        }
        bindSingleton {
            TemplateProjection(instance(), instance())
        }
        bind<EventRepository<TemplateEvent>>() with singleton {
            TemplateEventRepositoryImpl(instance(), instance())
        }
        bind<EventRepository<ChecklistEvent>>() with singleton {
            ChecklistEventRepositoryImpl(instance(), instance())
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
    }
