package tech.checkstory

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
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

class TemplateTest {

    @Test
    fun `given fresh account, then should return empty checklists`() = mockApplication {
        // given
        jsonClient.mockAuthorized().post("/auth/register")

        // when
        val response = jsonClient.mockAuthorized().get("checklists")

        // then
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(emptyList<Checklist>(), response.body())
    }

    @Test
    fun `when no account provided, then should respond with http unauthorized`() = mockApplication {
        val response = jsonClient.get("checklists")
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `given a template was created, when templates called, then should return created template`() = mockApplication(
        setupOperations = {
            val giftedSubscriptionRepository by instance<GiftedSubscriptionRepository>()
            runBlocking {
                giftedSubscriptionRepository.giftSubscriptionTo("mock-user-id", "Unit tests")
            }
        }
    ) {
        jsonClient.mockAuthorized().post("/auth/register")

        // when
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

        // then
        val response = jsonClient.mockAuthorized().get("templates")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(templateId, response.body<List<Template>>().first().id)
    }
}

