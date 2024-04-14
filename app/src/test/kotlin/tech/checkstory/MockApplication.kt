package tech.checkstory

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.testing.*
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import org.kodein.di.DI
import org.kodein.di.instance
import tech.checkstory.command.CommandsPayload
import tech.checkstory.core.domain.auth.UserService
import tech.checkstory.payment.domain.GiftedSubscriptionRepository
import tech.checkstory.template.domain.command.TemplateCommand
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
            json(Json {
                encodeDefaults = true
                prettyPrint = true
                explicitNulls = false
            })
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

suspend fun ApplicationTestBuilder.existingTemplate(): UUID {
    val templateId = UUID.randomUUID()
    jsonClient.mockAuthorized().post("sync/commands") {
        setBody(
            CommandsPayload(
                templateCommands = listOf(
                    TemplateCommand.CreateTemplateCommand(
                        templateId = templateId,
                        commandId = UUID.randomUUID(),
                        timestamp = Clock.System.now(),
                        existingTemplateData = null
                    )
                ),
                checklistCommands = emptyList()
            )
        )
    }
    return templateId
}
