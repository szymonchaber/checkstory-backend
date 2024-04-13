import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.9.10"
    id("io.ktor.plugin") version "2.2.4"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.8.20"
    id("io.sentry.jvm.gradle") version "4.3.0"
}

group = "tech.checkstory"
version = "0.0.1"
application {
    mainClass.set("tech.checkstory.ApplicationKt")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    languageVersion = "1.9"
    jvmTarget = "17"
    freeCompilerArgs += "-Xcontext-receivers"
}

tasks.named("shadowJar", ShadowJar::class.java) {
    mergeServiceFiles()
}

dependencies {
    implementation(project(":core:monolith"))
    implementation(project(":core:domain"))
    implementation(project(":template:module"))
    implementation(project(":template:domain"))
    implementation(project(":template:data"))
    implementation(project(":auth:module"))
    implementation(project(":auth:domain"))
    implementation(project(":auth:data"))
    implementation(project(":checklist:module"))
    implementation(project(":checklist:domain"))
    implementation(project(":checklist:data"))

    implementation(libs.ktor.server.core.jvm)
    implementation(libs.ktor.server.netty.jvm)
    implementation(libs.ktor.server.auth.jvm)
    implementation(libs.bundles.exposed)
    implementation(libs.hikari)
    implementation(libs.bundles.ktor.config)
    implementation(libs.kodein)
    implementation(libs.firebase.admin)
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:2.3.2")

    implementation(platform("io.opentelemetry:opentelemetry-bom:1.35.0"));
    implementation("io.opentelemetry.instrumentation:opentelemetry-ktor-2.0:2.1.0-alpha")
    implementation("io.opentelemetry:opentelemetry-api")
    implementation("io.opentelemetry:opentelemetry-sdk")
    implementation("io.opentelemetry:opentelemetry-exporter-logging")

    implementation("io.sentry:sentry-opentelemetry-core:7.3.0")

    testImplementation("io.ktor:ktor-client-content-negotiation:2.3.8")
    testImplementation("io.ktor:ktor-server-test-host-jvm:2.3.2")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:1.9.22")

}
