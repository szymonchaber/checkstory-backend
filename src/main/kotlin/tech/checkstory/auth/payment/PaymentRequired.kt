package tech.checkstory.auth.payment

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import tech.checkstory.auth.user.User

val PaymentRequiredPlugin = createRouteScopedPlugin(
    name = "PaymentRequiredPlugin"
) {
    pluginConfig.apply {
        on(AuthenticationChecked) { call ->
            val user = call.principal<User>()
            if (user == null) {
                call.respond(HttpStatusCode.Unauthorized)
                return@on
            }
            if (!user.isPaidUser) {
                call.respondText("You are not subscribed to any tier", status = HttpStatusCode.PaymentRequired)
            }
        }
    }
}

fun Route.requirePayment() {
    install(PaymentRequiredPlugin)
}
