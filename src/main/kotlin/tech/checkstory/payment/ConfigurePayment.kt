package tech.checkstory.payment

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import tech.checkstory.auth.firebase.FIREBASE_AUTH
import tech.checkstory.auth.withPrincipal
import tech.checkstory.payment.api.StoreGooglePlayTokenPayload
import tech.checkstory.payment.data.PaymentRepository
import tech.checkstory.payment.domain.GooglePlayPaymentToken
import tech.checkstory.payment.domain.StoreGooglePlayTokenUseCase
import tech.checkstory.payment.domain.StoreTokenResult

fun Application.configurePayment(
    paymentRepository: PaymentRepository
) {
    val storeGooglePlayTokenUseCase = StoreGooglePlayTokenUseCase(paymentRepository)
    routing {
        authenticate(FIREBASE_AUTH) {
            post<StoreGooglePlayTokenPayload>("/payment/google-play-token") {
                withPrincipal {
                    when (storeGooglePlayTokenUseCase.storePaymentToken(GooglePlayPaymentToken(it.token))) {
                        StoreTokenResult.Success -> call.respond(HttpStatusCode.OK)
                        StoreTokenResult.TokenAssignedToAnotherUser -> call.respond(HttpStatusCode.Conflict)
                    }
                }
            }
        }
    }
}