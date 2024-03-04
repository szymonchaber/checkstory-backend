package tech.checkstory.auth.firebase

import com.google.firebase.auth.FirebaseToken
import io.ktor.http.auth.*
import io.ktor.server.auth.*
import io.ktor.server.request.*

class FirebaseConfig(
    name: String?,
    val firebaseAuthenticationFunction: AuthenticationFunction<FirebaseToken>
) : AuthenticationProvider.Config(name)

fun ApplicationRequest.parseAuthorizationHeaderOrNull(): HttpAuthHeader? {
    return try {
        parseAuthorizationHeader()
    } catch (exception: IllegalArgumentException) {
        println("failed to parse token")
        null
    }
}

