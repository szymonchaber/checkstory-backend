package tech.checkstory

import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import tech.checkstory.checklist.domain.aggregate.Checklist
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
    fun `when no account provided, then should unauthorized`() = mockApplication {
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
}
