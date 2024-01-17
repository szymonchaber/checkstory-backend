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
    implementation(project(":core:domain"))
    implementation(project(":template:domain"))
    implementation(project(":core:monolith"))
    implementation(libs.kodein)
    implementation(libs.bundles.exposed)
    implementation(libs.kotlinx.datetime)
    implementation(libs.ktor.server.auth.jvm)
    testImplementation(libs.bundles.test)
}
