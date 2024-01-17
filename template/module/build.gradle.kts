import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.9.10"
    id("io.ktor.plugin") version "2.2.4"
}

val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    languageVersion = "1.9"
    jvmTarget = "17"
    freeCompilerArgs += "-Xcontext-receivers"
}

dependencies {
    implementation(project(":template:domain"))
    implementation(project(":template:data"))
    implementation(project(":core:monolith"))
    implementation(libs.ktor.server.core.jvm)
    implementation(libs.ktor.server.auth.jvm)
    implementation(libs.kodein)
    testImplementation(libs.bundles.test)
}
