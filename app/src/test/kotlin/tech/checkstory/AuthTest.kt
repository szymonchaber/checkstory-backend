package tech.checkstory

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import tech.checkstory.auth.module.UserDto
import kotlin.test.Test
import kotlin.test.assertEquals

class AuthTest {

    @Test
    fun `when register called, then should register an account`() = mockApplication {
        val response = jsonClient.post("/auth/register") {
            header("Authorization", "Bearer example-token")
        }
        assertEquals(HttpStatusCode.Created, response.status)
    }

    @Test
    fun `given account exists, when login called, then should return account details`() = mockApplication {
        // given
        jsonClient.post("/auth/register") {
            header("Authorization", "Bearer example-token")
        }

        // when
        val response = jsonClient.get("/auth/login") {
            header("Authorization", "Bearer example-token")
        }

        // then
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(UserDto(userId = "mock-user-id", isPaidUser = false), response.body<UserDto>())
    }
}
