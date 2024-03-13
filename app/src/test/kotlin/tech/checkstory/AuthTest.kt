package tech.checkstory

import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import org.kodein.di.instance
import tech.checkstory.checklist.domain.aggregate.Checklist
import tech.checkstory.command.CommandsPayload
import tech.checkstory.payment.data.GiftedSubscriptionRepository
import tech.checkstory.template.domain.aggregate.Template
import tech.checkstory.template.domain.command.TemplateCommand
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals

class AuthTest {

    @Test
    fun `when register called, then should register an account`() = mockApplication {
        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }
        val response = client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer example-token")
        }
        assertEquals(HttpStatusCode.Created, response.status)
    }

    @Test
    fun `when an account is new, then should return empty checklists`() = mockApplication {
        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }
        client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer example-token")
        }
        val response = client.get("checklists") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer example-token")
        }
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(emptyList<Checklist>(), response.body())
    }

    @Test
    fun `when no account provided, then should respond with http unauthorized`() = mockApplication {
        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }
        val response = client.get("checklists") {
            contentType(ContentType.Application.Json)
        }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `given a template was created, then should return that template`() = mockApplication(setupOperations = {
        val giftedSubscriptionRepository by instance<GiftedSubscriptionRepository>()
        runBlocking {
            giftedSubscriptionRepository.giftSubscriptionTo("mock-user-id", "Unit tests")
        }
    }) {
        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }
        client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer example-token")
        }
        val templateId = UUID.randomUUID()
        val post = client.post("sync/commands") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer example-token")
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
        val response = client.get("templates") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer example-token")
        }
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(templateId, response.body<List<Template>>().first().id)
    }
}
