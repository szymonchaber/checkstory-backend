package tech.checkstory.auth.firebase

import io.ktor.http.auth.*
import io.ktor.server.auth.*
import io.ktor.server.request.*

class FirebaseConfig(
    name: String?,
    val firebaseAuthenticationFunction: AuthenticationFunction<FirebaseUserId>
) : AuthenticationProvider.Config(name)

fun ApplicationRequest.parseAuthorizationHeaderOrNull(): HttpAuthHeader? {
    return try {
        parseAuthorizationHeader()
    } catch (exception: IllegalArgumentException) {
        println("failed to parse token")
        null
    }
}

@JvmInline
value class FirebaseUserId(val uid: String)
