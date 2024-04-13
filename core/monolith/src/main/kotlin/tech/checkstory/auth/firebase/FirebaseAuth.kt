package tech.checkstory.auth.firebase

import io.ktor.http.auth.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import tech.checkstory.core.domain.auth.UserService
import tech.checkstory.payment.domain.GetPaymentStateUseCase

const val FIREBASE_AUTH = "FIREBASE_AUTH"
private const val FirebaseJWTAuthKey: String = "FirebaseAuth"

class FirebaseAuthProvider(
    private val firebaseTokenExtractor: FirebaseTokenExtractor,
    private val userService: UserService,
    private val getPaymentStateUseCase: GetPaymentStateUseCase
) : AuthenticationProvider(FirebaseConfig(FIREBASE_AUTH)) {

    override suspend fun onAuthenticate(context: AuthenticationContext) {
        val token = context.call.request.parseAuthorizationHeaderOrNull()

        if (token == null) {
            context.challenge(
                FirebaseJWTAuthKey,
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
            context.error(FirebaseJWTAuthKey, AuthenticationFailedCause.InvalidCredentials)
        }
    }

    private fun HttpAuthHeader.Companion.bearerAuthChallenge(realm: String): HttpAuthHeader {
        return HttpAuthHeader.Parameterized("Bearer", mapOf(HttpAuthHeader.Parameters.Realm to realm))
    }
}

internal class FirebaseConfig(name: String?) : AuthenticationProvider.Config(name)
