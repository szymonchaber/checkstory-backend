package tech.checkstory.plugins

import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.request.*

fun Application.configureMonitoring() {
    install(CallLogging) {
        filter { call ->
            call.request.path().startsWith("/")
        }
//        configureCustomFormat()
    }
//    install(DropwizardMetrics) {
//        Slf4jReporter.forRegistry(registry)
//            .outputTo(Logger.getGlobal())
//            .convertRatesTo(TimeUnit.SECONDS)
//            .convertDurationsTo(TimeUnit.MILLISECONDS)
//            .build()
//            .start(10, TimeUnit.SECONDS)
//    }
}

private fun CallLoggingConfig.configureCustomFormat() {
    format { call ->
        StringBuilder().apply {
            appendLine("Received request:")
            val requestURI = call.request.origin.uri
            appendLine(call.request.origin.run { "${method.value} $scheme://$localHost:$localPort$requestURI $version" })

            call.request.headers.forEach { header, values ->
                appendLine("$header: ${values.firstOrNull()}")
            }
        }.toString()
    }
}
