package tech.checkstory.auth.firebase

import io.ktor.http.auth.*
import io.ktor.server.auth.*
import io.ktor.server.response.*

class FirebaseAuthProvider(
    private val config: FirebaseConfig,
    private val firebaseTokenExtractor: FirebaseTokenExtractor
) : AuthenticationProvider(config) {

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
            val principal = firebaseTokenExtractor.toFirebaseTokenOrNull(token)?.let {
                config.firebaseAuthenticationFunction(context.call, it)
            }

            if (principal != null) {
                context.principal(principal)
            }
        } catch (cause: Throwable) {
            val message = cause.message ?: cause.javaClass.simpleName
            context.error(FirebaseJWTAuthKey, AuthenticationFailedCause.Error(message))
        }
    }

}

fun HttpAuthHeader.Companion.bearerAuthChallenge(realm: String): HttpAuthHeader =
    HttpAuthHeader.Parameterized("Bearer", mapOf(HttpAuthHeader.Parameters.Realm to realm))

const val FIREBASE_AUTH = "FIREBASE_AUTH"
const val FirebaseJWTAuthKey: String = "FirebaseAuth"

