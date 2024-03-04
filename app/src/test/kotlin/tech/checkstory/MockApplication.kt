package tech.checkstory

import io.ktor.server.testing.*

internal fun mockApplication(block: suspend ApplicationTestBuilder.() -> Unit) {
    testApplication {
        application {
            mockModule()
        }
        block()
    }
}
