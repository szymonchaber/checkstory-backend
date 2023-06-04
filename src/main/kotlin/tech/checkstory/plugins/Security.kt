package tech.checkstory.plugins

import io.ktor.server.application.*
import io.ktor.server.auth.*
import org.kodein.di.DI
import org.kodein.di.instance
import tech.checkstory.auth.firebase.firebase
import tech.checkstory.auth.user.UserService
import tech.checkstory.payment.domain.GetPaymentStateUseCase

context(DI)
fun Application.configureSecurity() {
    val userService by instance<UserService>()
    val getPaymentStateUseCase by instance<GetPaymentStateUseCase>()
    authentication {
        firebase {
            validate {
                val isPaidUser = getPaymentStateUseCase.getPaymentStateForUser(it.uid).isSubscriptionActive
                userService.readById(it.uid)?.copy(isPaidUser = isPaidUser)
                    ?: error("Provided user token is not a registered user")
            }
        }
    }
}
