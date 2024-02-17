package tech.checkstory

import io.ktor.server.application.*
import io.opentelemetry.context.propagation.ContextPropagators
import io.opentelemetry.instrumentation.ktor.v2_0.server.KtorServerTracing
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.sentry.Instrumenter
import io.sentry.Sentry
import io.sentry.opentelemetry.OpenTelemetryLinkErrorEventProcessor
import io.sentry.opentelemetry.SentryPropagator
import io.sentry.opentelemetry.SentrySpanProcessor


fun Application.openTelemetry() {
    val sdkTracerProvider = SdkTracerProvider.builder()
        .addSpanProcessor(SentrySpanProcessor())
        .build()

    val openTelemetry = OpenTelemetrySdk.builder()
        .setTracerProvider(sdkTracerProvider)
        .setPropagators(ContextPropagators.create(SentryPropagator()))
        .buildAndRegisterGlobal()
    Sentry.init { options ->
        options.dsn = "https://783c9621a8b6a65a666d928786e14180@o4506758951993344.ingest.sentry.io/4506758954745856"
        options.tracesSampleRate = 1.0
        options.instrumenter = Instrumenter.OTEL
        options.addEventProcessor(OpenTelemetryLinkErrorEventProcessor())
        options.isDebug = true
    }
    install(KtorServerTracing) {
        setOpenTelemetry(openTelemetry)
    }
}
