import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.9.10"
    id("io.ktor.plugin") version "2.2.4"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.8.20"
}

val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    languageVersion = "1.9"
    jvmTarget = "17"
    freeCompilerArgs += "-Xcontext-receivers"
}

dependencies {
    implementation(project(":payment:domain"))
    implementation(project(":payment:data"))
    implementation(project(":core:monolith"))
    implementation(project(":core:domain"))
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.0")

    implementation(libs.ktor.server.core.jvm)
    implementation(libs.ktor.server.auth.jvm)
    implementation(libs.kodein)
    testImplementation(libs.bundles.test)
}
