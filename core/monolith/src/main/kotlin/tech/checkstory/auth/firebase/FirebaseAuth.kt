package tech.checkstory.auth.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseToken
import io.ktor.http.auth.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FirebaseAuthProvider(private val config: FirebaseConfig) : AuthenticationProvider(config) {

    override suspend fun onAuthenticate(context: AuthenticationContext) {
        val token = context.call.request.parseAuthorizationHeaderOrNull()

        if (token == null) {
            context.challenge(
                FirebaseJWTAuthKey,
                AuthenticationFailedCause.InvalidCredentials
            ) { challengeFunc, call ->
                challengeFunc.complete()
                call.respond(UnauthorizedResponse(HttpAuthHeader.bearerAuthChallenge(realm = FIREBASE_AUTH)))
            }
            return
        }

        try {
            val principal = verifyFirebaseIdToken(context.call, token, config.firebaseAuthenticationFunction)

            if (principal != null) {
                context.principal(principal)
            }
        } catch (cause: Throwable) {
            val message = cause.message ?: cause.javaClass.simpleName
            context.error(FirebaseJWTAuthKey, AuthenticationFailedCause.Error(message))
        }
    }
}

suspend fun verifyFirebaseIdToken(
    call: ApplicationCall,
    authHeader: HttpAuthHeader,
    principalFactory: suspend ApplicationCall.(FirebaseToken) -> Principal?
): Principal? {
    return authHeader.toFirebaseTokenOrNull()?.let {
        principalFactory(call, it)
    }
}

suspend fun HttpAuthHeader.toFirebaseTokenOrNull(): FirebaseToken? {
    return try {
        if (authScheme == "Bearer" && this is HttpAuthHeader.Single) {
            withContext(Dispatchers.IO) {
                FirebaseAuth.getInstance().verifyIdToken(blob)
            }
        } else {
            null
        }
    } catch (exception: Exception) {
        exception.printStackTrace()
        null
    }
}

fun HttpAuthHeader.Companion.bearerAuthChallenge(realm: String): HttpAuthHeader =
    HttpAuthHeader.Parameterized("Bearer", mapOf(HttpAuthHeader.Parameters.Realm to realm))

const val FIREBASE_AUTH = "FIREBASE_AUTH"
const val FirebaseJWTAuthKey: String = "FirebaseAuth"

