package tech.checkstory.auth.firebase

import io.ktor.server.auth.*
import io.ktor.server.routing.*

const val FIREBASE_AUTH = "FIREBASE_AUTH"

fun Route.authenticateFirebase(build: Route.() -> Unit): Route {
    return authenticate(FIREBASE_AUTH, build = build)
}
