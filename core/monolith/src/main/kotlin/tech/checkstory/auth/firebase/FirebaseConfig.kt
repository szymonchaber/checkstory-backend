package tech.checkstory.auth.firebase

import com.google.firebase.auth.FirebaseToken
import io.ktor.http.auth.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import tech.checkstory.core.domain.auth.User

class FirebaseConfig(name: String?) : AuthenticationProvider.Config(name) {

    internal var authHeader: (ApplicationCall) -> HttpAuthHeader? =
        { call ->
            call.request.parseAuthorizationHeaderOrNull()
        }

    var firebaseAuthenticationFunction: AuthenticationFunction<FirebaseToken> = {
        throw NotImplementedError(FirebaseImplementationError)
    }

    fun validate(validate: suspend ApplicationCall.(FirebaseToken) -> User?) {
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
