package tech.checkstory.app.auth.firebase

import io.ktor.http.auth.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import tech.checkstory.auth.firebase.FIREBASE_AUTH
import tech.checkstory.auth.firebase.FirebaseTokenExtractor
import tech.checkstory.auth.firebase.parseAuthorizationHeaderOrNull
import tech.checkstory.core.domain.auth.UserService
import tech.checkstory.payment.domain.GetPaymentStateUseCase

class FirebaseAuthProvider(
    private val firebaseTokenExtractor: FirebaseTokenExtractor,
    private val userService: UserService,
    private val getPaymentStateUseCase: GetPaymentStateUseCase
) : AuthenticationProvider(FirebaseConfig(FIREBASE_AUTH)) {

    override suspend fun onAuthenticate(context: AuthenticationContext) {
        val token = context.call.request.parseAuthorizationHeaderOrNull()

        if (token == null) {
            context.challenge(
                FIREBASE_JWT_AUTH_KEY,
                AuthenticationFailedCause.NoCredentials
            ) { challengeFunc, call ->
                challengeFunc.complete()
                call.respond(UnauthorizedResponse(HttpAuthHeader.bearerAuthChallenge(realm = FIREBASE_AUTH)))
            }
            return
        }

        val principal = firebaseTokenExtractor.toFirebaseTokenOrNull(token)?.let {
            val isPaidUser = getPaymentStateUseCase.getPaymentStateForUser(it.uid).isSubscriptionActive
            userService.readById(it.uid)
                ?.copy(isPaidUser = isPaidUser)
        }

        if (principal != null) {
            context.principal(principal)
        } else {
            context.error(FIREBASE_JWT_AUTH_KEY, AuthenticationFailedCause.InvalidCredentials)
        }
    }

    private fun HttpAuthHeader.Companion.bearerAuthChallenge(realm: String): HttpAuthHeader {
        return HttpAuthHeader.Parameterized("Bearer", mapOf(HttpAuthHeader.Parameters.Realm to realm))
    }

    companion object {

        private const val FIREBASE_JWT_AUTH_KEY: String = "FirebaseAuth"
    }
}

internal class FirebaseConfig(name: String?) : AuthenticationProvider.Config(name)
