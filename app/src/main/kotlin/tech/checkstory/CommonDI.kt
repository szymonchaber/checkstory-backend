package tech.checkstory

import io.ktor.server.application.*
import io.ktor.server.config.*
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.Database
import org.kodein.di.*
import tech.checkstory.app.auth.firebase.FirebaseAuthProvider
import tech.checkstory.auth.data.UserServiceImpl
import tech.checkstory.auth.firebase.FirebaseTokenExtractor
import tech.checkstory.auth.firebase.FirebaseTokenExtractorImpl
import tech.checkstory.checklist.data.ChecklistRepository
import tech.checkstory.core.domain.auth.DeleteUserUseCase
import tech.checkstory.core.domain.auth.UserService
import tech.checkstory.notification.data.FirebaseMessagingTokenRepository
import tech.checkstory.notification.firebase.UserDataChangeNotifier
import tech.checkstory.payment.data.google_play.SubscriptionsGoogleApi
import tech.checkstory.payment.domain.GetPaymentStateUseCase
import tech.checkstory.payment.domain.GiftedSubscriptionRepository
import tech.checkstory.payment.domain.PurchaseRepository
import tech.checkstory.payment.domain.StoreGooglePlayTokenUseCase
import tech.checkstory.payment.playbilling.PlayBillingPubSubHandler
import tech.checkstory.plugins.createHikariDataSource
import tech.checkstory.template.data.TemplateRepository

val Application.commonDI
    get() = DI.Module("common") {
        bind<ApplicationConfig>() with singleton { environment.config }
        bindSingleton<Database> {
            val connectionString = instance<ApplicationConfig>().property("postgres.connectionString").getString()
            require(connectionString.isNotEmpty()) {
                "Postgres connection string is empty, which means some configuration was incorrect"
            }
            val database =
                Database.connect(datasource = createHikariDataSource(connectionString, "org.postgresql.Driver"))
            database
        }
        bindSingleton {
            Json {
                encodeDefaults = true
                explicitNulls = false
                prettyPrint = true
            }
        }
        bindSingleton {
            TemplateRepository(instance())
        }
        bindSingleton<UserService> {
            UserServiceImpl(instance())
        }
        bindSingleton<FirebaseTokenExtractor> {
            FirebaseTokenExtractorImpl()
        }
        bindSingleton {
            FirebaseAuthProvider(
                instance(),
                instance(),
                instance(),
            )
        }
        bindSingleton<DeleteUserUseCase> {
            DeleteUserUseCaseImpl(
                instance(),
                instance(),
                instance(),
                instance(),
                instance(),
                instance(),
            )
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
            ChecklistRepository(instance())
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
            StoreGooglePlayTokenUseCase(instance(), instance())
        }
        bindSingleton {
            GetPaymentStateUseCase(instance(), instance(), instance())
        }
        bindSingleton {
            val subscriptionName = instance<ApplicationConfig>().property("playBilling.subscriptionName").getString()
            require(subscriptionName.isNotEmpty()) {
                "Play Billing Pub/Sub subscription name is empty, which means some configuration is incorrect"
            }
            PlayBillingPubSubHandler(
                subscriptionsGoogleApi = instance(),
                purchaseRepository = instance(),
                notifier = instance(),
                subscriptionName = subscriptionName
            )
        }
    }
