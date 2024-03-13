package tech.checkstory

import io.ktor.server.application.*
import org.kodein.di.DI
import tech.checkstory.auth.firebase.Firebase

fun Application.productionModule() {
    println("Starting production module")
    DI {
        import(commonDI)
    }.apply {
        openTelemetry()
        Firebase.init()
        module()
    }
}
