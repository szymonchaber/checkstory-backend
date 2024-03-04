package tech.checkstory.auth.firebase

import com.google.firebase.auth.FirebaseToken
import io.ktor.http.auth.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import tech.checkstory.core.domain.auth.User

class FirebaseConfig(name: String?) : AuthenticationProvider.Config(name) {

    var firebaseAuthenticationFunction: AuthenticationFunction<FirebaseToken> = {
        throw NotImplementedError("Firebase auth validate function is not specified, use firebase { validate { ... } } to fix this")
    }

    fun validate(validate: suspend ApplicationCall.(FirebaseToken) -> User?) {
        firebaseAuthenticationFunction = validate
    }
}

fun ApplicationRequest.parseAuthorizationHeaderOrNull(): HttpAuthHeader? {
    return try {
        parseAuthorizationHeader()
    } catch (exception: IllegalArgumentException) {
        println("failed to parse token")
        null
    }
}

