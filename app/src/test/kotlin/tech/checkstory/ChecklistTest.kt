package tech.checkstory

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import tech.checkstory.checklist.domain.aggregate.Checklist
import kotlin.test.Test
import kotlin.test.assertEquals

class ChecklistTest {

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
}
