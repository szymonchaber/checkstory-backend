package tech.checkstory.plugins

import io.ktor.server.application.*
import io.ktor.server.auth.*
import org.kodein.di.DI
import org.kodein.di.instance
import tech.checkstory.auth.firebase.FIREBASE_AUTH
import tech.checkstory.auth.firebase.FirebaseAuthProvider
import tech.checkstory.auth.firebase.FirebaseConfig
import tech.checkstory.core.domain.auth.UserService
import tech.checkstory.payment.domain.GetPaymentStateUseCase

context(DI)
fun Application.configureSecurity() {
    val userService by instance<UserService>()
    val getPaymentStateUseCase by instance<GetPaymentStateUseCase>()
    authentication {
        val provider = FirebaseAuthProvider(FirebaseConfig(FIREBASE_AUTH) {
            val isPaidUser = getPaymentStateUseCase.getPaymentStateForUser(it.uid).isSubscriptionActive
            userService.readById(it.uid)
                ?.copy(isPaidUser = isPaidUser)
                ?: error("Provided user token is not a registered user")
        })
        register(provider)
    }
}
