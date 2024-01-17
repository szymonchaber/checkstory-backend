package tech.checkstory.plugins

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import kotlinx.serialization.json.Json
import org.kodein.di.DI
import org.kodein.di.instance

context(DI)
fun Application.configureSerialization() {
    install(ContentNegotiation) {
        val json by instance<Json>()
        json(json)
    }
}
