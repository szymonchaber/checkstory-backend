package tech.checkstory

import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
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
}
