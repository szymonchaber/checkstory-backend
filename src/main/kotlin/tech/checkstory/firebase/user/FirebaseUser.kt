package tech.checkstory.firebase.user

import com.google.firebase.auth.FirebaseToken
import io.ktor.http.auth.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*

data class FirebaseUser(val userId: String = "", val displayName: String = "") : Principal

class FirebaseConfig(name: String?) : AuthenticationProvider.Config(name) {

    internal var authHeader: (ApplicationCall) -> HttpAuthHeader? =
        { call ->
            call.request.parseAuthorizationHeaderOrNull()
        }

    var firebaseAuthenticationFunction: AuthenticationFunction<FirebaseToken> = {
        throw NotImplementedError(FirebaseImplementationError)
    }

    fun validate(validate: suspend ApplicationCall.(FirebaseToken) -> FirebaseUser?) {
        firebaseAuthenticationFunction = validate
    }
}

fun ApplicationRequest.parseAuthorizationHeaderOrNull(): HttpAuthHeader? = try {
    parseAuthorizationHeader()
} catch (exception: IllegalArgumentException) {
    println("failed to parse token")
    null
}

private const val FirebaseImplementationError =
    "Firebase auth validate function is not specified, use firebase { validate { ... } } to fix this"