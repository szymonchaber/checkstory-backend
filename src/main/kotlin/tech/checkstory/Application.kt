package tech.checkstory

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import org.kodein.di.DI

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::productionModule).start(wait = true)
}

fun Application.productionModule() {
    DI {
        import(commonDI)
    }.apply {
        module()
    }
}
