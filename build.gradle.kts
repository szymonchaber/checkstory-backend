import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val ktor_version: String by project
val kotlin_version: String by project
val logback_version: String by project
val postgres_version: String by project
val h2_version: String by project
val exposed_version: String by project

plugins {
    kotlin("jvm") version "1.8.20"
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

ktor {
    docker {
        jreVersion.set(io.ktor.plugin.features.JreVersion.JRE_17)
        localImageName.set("checkstory-docker-image")
        imageTag.set("0.0.1-preview")
    }
}

repositories {
    mavenCentral()
}
tasks.named("compileKotlin", org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask::class.java) {
    compilerOptions {
        freeCompilerArgs.add("-Xcontext-receivers")
    }
}

tasks.named("shadowJar", ShadowJar::class.java) {
    mergeServiceFiles()
}

dependencies {
    implementation("org.postgresql:postgresql:$postgres_version")
    implementation("com.h2database:h2:$h2_version")
    implementation("org.jetbrains.exposed:exposed-core:$exposed_version")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposed_version")
    implementation("ch.qos.logback:logback-classic:$logback_version")

    implementation("com.eventstore:db-client-java:4.1.1")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.0")

    implementation("com.google.firebase:firebase-admin:9.1.1")
    implementation("io.ktor:ktor-server-core-jvm:2.3.0")
    implementation("io.ktor:ktor-server-auth-jvm:2.3.0")
    implementation("io.ktor:ktor-client-core-jvm:2.3.0")
    implementation("io.ktor:ktor-client-apache-jvm:2.3.0")
    implementation("io.ktor:ktor-server-host-common-jvm:2.3.0")
    implementation("io.ktor:ktor-server-status-pages-jvm:2.3.0")
    implementation("io.ktor:ktor-server-default-headers-jvm:2.3.0")
    implementation("io.ktor:ktor-server-http-redirect-jvm:2.3.0")
    implementation("io.ktor:ktor-server-cors-jvm:2.3.0")
    implementation("io.ktor:ktor-server-openapi:2.3.0")
    implementation("io.ktor:ktor-server-swagger:2.3.0")
    implementation("io.ktor:ktor-server-call-logging-jvm:2.3.0")
    implementation("io.ktor:ktor-server-metrics-jvm:2.3.0")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:2.3.0")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:2.3.0")
    implementation("io.ktor:ktor-server-netty-jvm:2.3.0")

    implementation(platform("com.google.cloud:libraries-bom:26.14.0"))
    implementation("com.google.cloud:google-cloud-pubsub")

    implementation("com.google.api-client:google-api-client:1.32.1")
    implementation("com.google.apis:google-api-services-androidpublisher:v3-rev20230511-2.0.0")
    implementation("com.google.api-client:google-api-client:1.32.1")

    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlin_version")
    testImplementation("io.ktor:ktor-server-tests-jvm:2.3.0")
    testImplementation("io.ktor:ktor-server-test-host-jvm:2.3.0")
}
val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    languageVersion = "1.9"
}