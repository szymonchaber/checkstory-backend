package tech.checkstory

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.testing.*
import kotlinx.coroutines.runBlocking
import org.kodein.di.DI
import org.kodein.di.instance
import tech.checkstory.core.domain.auth.UserService
import tech.checkstory.payment.data.GiftedSubscriptionRepository
import java.util.*

internal fun mockApplication(setupOperations: DI.() -> Unit = {}, block: suspend ApplicationTestBuilder.() -> Unit) {
    val database = mockDatabase(UUID.randomUUID().toString())
    testApplication {
        application {
            mockModule(setupOperations, database)
        }
        block()
    }
}

internal fun mockApplicationWithUser(
    isUserPaid: Boolean = false,
    setupOperations: DI.() -> Unit = {},
    block: suspend ApplicationTestBuilder.(userId: String) -> Unit
) {
    mockApplication(
        setupOperations = {
            val userService by instance<UserService>()
            runBlocking {
                userService.create("mock-user-id")
            }
            if (isUserPaid) {
                val giftedSubscriptionRepository by instance<GiftedSubscriptionRepository>()
                runBlocking {
                    giftedSubscriptionRepository.giftSubscriptionTo("mock-user-id", "Unit tests")
                }
            }
            setupOperations()
        }
    ) {
        block("mock-user-id")
    }
}

val ApplicationTestBuilder.jsonClient: HttpClient
    get() = createClient {
        install(ContentNegotiation) {
            json()
        }
        defaultRequest {
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
        }
    }

fun HttpClient.mockAuthorized(): HttpClient = config {
    defaultRequest {
        header("Authorization", "Bearer example-token")
    }
}
