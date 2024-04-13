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
    implementation(project(":core:domain"))
    implementation(project(":core:monolith"))
    implementation(project(":template:domain"))
    implementation(libs.ktor.server.auth.jvm)

    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:2.3.2")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.0")

    testImplementation("io.ktor:ktor-server-tests-jvm")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:1.9.10")
}
