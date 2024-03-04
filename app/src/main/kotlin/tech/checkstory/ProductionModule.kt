package tech.checkstory

import io.ktor.server.application.*
import org.kodein.di.DI

fun Application.productionModule() {
    println("Starting production module")
    DI {
        import(commonDI)
    }.apply {
        openTelemetry()
        module()
    }
}
