package tech.checkstory

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.testing.*
import org.kodein.di.DI

internal fun mockApplication(setupOperations: DI.() -> Unit = {}, block: suspend ApplicationTestBuilder.() -> Unit) {
    testApplication {
        application {
            mockModule(setupOperations)
        }
        block()
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
