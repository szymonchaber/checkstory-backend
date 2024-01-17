import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.9.10"
    id("io.ktor.plugin") version "2.2.4"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.8.20"
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
    implementation(project(":core:domain"))
    implementation(libs.postgres)
    implementation(libs.h2)
    implementation(libs.bundles.exposed)
    implementation(libs.logback)

    implementation(libs.eventstoredb)
    implementation(libs.kotlinx.datetime)

    implementation(libs.firebase.admin)

    implementation(platform(libs.google.cloud.bom))
    implementation(libs.google.cloud.pubsub)

    implementation(libs.bundles.google.play)

    implementation(libs.calendar.rrule)

    implementation(libs.retry)
    implementation(libs.ktor.server.core.jvm)
    implementation(libs.kodein)
    implementation(libs.ktor.server.auth.jvm)
    implementation(libs.bundles.ktor.config)

    implementation(libs.ktor.client.core.jvm)
    implementation(libs.ktor.client.apache.jvm)

    testImplementation(libs.bundles.test)
}
