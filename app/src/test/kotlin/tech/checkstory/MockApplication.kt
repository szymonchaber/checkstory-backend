package tech.checkstory

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
