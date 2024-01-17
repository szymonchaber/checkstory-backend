package tech.checkstory.payment

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.kodein.di.DI
import org.kodein.di.instance
import tech.checkstory.auth.firebase.FIREBASE_AUTH
import tech.checkstory.auth.withPrincipal
import tech.checkstory.payment.api.StoreGooglePlayTokenPayload
import tech.checkstory.payment.domain.GooglePlayPaymentToken
import tech.checkstory.payment.domain.StoreGooglePlayTokenUseCase
import tech.checkstory.payment.domain.StoreTokenResult

context(DI)
fun Application.configurePayment() {
    val storeGooglePlayTokenUseCase by instance<StoreGooglePlayTokenUseCase>()
    routing {
        authenticate(FIREBASE_AUTH) {
            post<StoreGooglePlayTokenPayload>("/payment/google-play-token") {
                withPrincipal {
                    when (storeGooglePlayTokenUseCase.storePaymentToken(
                        principal.userId,
                        GooglePlayPaymentToken(it.token)
                    )) {
                        StoreTokenResult.Success -> call.respond(HttpStatusCode.OK)
                        StoreTokenResult.TokenAssignedToAnotherUser -> call.respond(HttpStatusCode.Conflict)
                    }
                }
            }
        }
    }
}
