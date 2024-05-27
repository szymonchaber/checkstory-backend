package tech.checkstory.notification

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.kodein.di.DI
import org.kodein.di.instance
import tech.checkstory.auth.firebase.authenticateFirebase
import tech.checkstory.auth.withPrincipal
import tech.checkstory.notification.api.FirebaseMessagingTokenPayload
import tech.checkstory.notification.data.FirebaseMessagingTokenRepository
import tech.checkstory.notification.domain.FirebaseMessagingToken
import tech.checkstory.notification.domain.StoreFirebaseMessagingTokenUseCase
import tech.checkstory.routingV1

context(DI)
fun Application.configureNotification() {
    val firebaseMessagingTokenRepository by instance<FirebaseMessagingTokenRepository>()
    val storeFirebaseTokenUseCase = StoreFirebaseMessagingTokenUseCase(firebaseMessagingTokenRepository)
    routingV1 {
        authenticateFirebase {
            post<FirebaseMessagingTokenPayload>("/notification/firebase-token") {
                withPrincipal {
                    storeFirebaseTokenUseCase.storeFirebaseMessagingToken(FirebaseMessagingToken(it.token))
                    call.respond(HttpStatusCode.OK)
                }
            }
        }
    }
}
