package tech.checkstory.notification

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import tech.checkstory.auth.firebase.FIREBASE_AUTH
import tech.checkstory.auth.withPrincipal
import tech.checkstory.notification.api.FirebaseMessagingTokenPayload
import tech.checkstory.notification.data.FirebaseMessagingTokenRepository
import tech.checkstory.notification.domain.FirebaseMessagingToken
import tech.checkstory.notification.domain.StoreFirebaseMessagingTokenUseCase

fun Application.configureNotification(
    firebaseMessagingTokenRepository: FirebaseMessagingTokenRepository
) {
    val storeFirebaseTokenUseCase = StoreFirebaseMessagingTokenUseCase(firebaseMessagingTokenRepository)
    routing {
        authenticate(FIREBASE_AUTH) {
            post<FirebaseMessagingTokenPayload>("/notification/firebase-token") {
                withPrincipal {
                    storeFirebaseTokenUseCase.storeFirebaseMessagingToken(FirebaseMessagingToken(it.token))
                    call.respond(HttpStatusCode.OK)
                }
            }
        }
    }
}
