package tech.checkstory.plugins

import io.ktor.server.application.*
import io.ktor.server.plugins.forwardedheaders.*
import org.kodein.di.DI

context(DI)
fun Application.configureHeaders() {
    install(ForwardedHeaders)
    install(XForwardedHeaders) {
        useFirstProxy()
    }
}
