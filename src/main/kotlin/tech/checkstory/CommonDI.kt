package tech.checkstory

import io.ktor.server.application.*
import io.ktor.server.config.*
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.Database
import org.kodein.di.*
import tech.checkstory.auth.user.UserService
import tech.checkstory.checklist.data.ChecklistEventMapper
import tech.checkstory.checklist.data.ChecklistEventRepositoryImpl
import tech.checkstory.checklist.domain.RelationalChecklistRepository
import tech.checkstory.checklist.domain.event.ChecklistEvent
import tech.checkstory.data.EventMapper
import tech.checkstory.data.EventRepository
import tech.checkstory.data.EventStoreConnectionManager
import tech.checkstory.eventbus.EventBus
import tech.checkstory.notification.data.FirebaseMessagingTokenRepository
import tech.checkstory.notification.firebase.UserDataChangeNotifier
import tech.checkstory.payment.data.GiftedSubscriptionRepository
import tech.checkstory.payment.data.PurchaseRepository
import tech.checkstory.payment.data.google_play.SubscriptionsGoogleApi
import tech.checkstory.payment.domain.GetPaymentStateUseCase
import tech.checkstory.payment.domain.StoreGooglePlayTokenUseCase
import tech.checkstory.plugins.connectToPostgres
import tech.checkstory.read.checklist.ChecklistProjection
import tech.checkstory.read.subscription.EventStoreDBSubscriptionToAll
import tech.checkstory.read.subscription.SubscriptionCheckpointRepository
import tech.checkstory.read.subscription.SubscriptionToAll
import tech.checkstory.read.template.TemplateProjection
import tech.checkstory.sync.NotificationSubscription
import tech.checkstory.template.data.EventSourcedTemplateRepository
import tech.checkstory.template.data.RelationalTemplateRepository
import tech.checkstory.template.data.TemplateEventMapper
import tech.checkstory.template.data.TemplateEventRepositoryImpl
import tech.checkstory.template.domain.event.TemplateEvent

val Application.commonDI
    get() = DI.Module("common") {
        bind<ApplicationConfig>() with singleton { environment.config }
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
        bindSingleton<EventStoreConnectionManager> {
            val eventStorePassword = instance<ApplicationConfig>().property("eventStore.password").getString()
            EventStoreConnectionManager(eventStorePassword)
        }
        bindSingleton {
            SubscriptionCheckpointRepository(instance())
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
            GiftedSubscriptionRepository(instance())
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
        bindSingleton {
            ChecklistProjection(instance(), instance())
        }
        bind<SubscriptionToAll>() with singleton {
            EventStoreDBSubscriptionToAll(instance(), instance(), instance(), instance())
        }
        bind<EventRepository<TemplateEvent>>() with singleton {
            TemplateEventRepositoryImpl(instance(), instance())
        }
        bind<EventRepository<ChecklistEvent>>() with singleton {
            ChecklistEventRepositoryImpl(instance(), instance())
        }
        bindSingleton {
            RelationalChecklistRepository(instance())
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
            NotificationSubscription(instance(), instance())
        }
        bindSingleton {
            StoreGooglePlayTokenUseCase(instance(), instance())
        }
        bindSingleton {
            GetPaymentStateUseCase(instance(), instance(), instance())
        }
    }
