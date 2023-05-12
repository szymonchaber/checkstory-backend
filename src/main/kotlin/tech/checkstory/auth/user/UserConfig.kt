package tech.checkstory.auth.user

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import tech.checkstory.auth.firebase.FIREBASE_AUTH
import tech.checkstory.auth.firebase.parseAuthorizationHeaderOrNull
import tech.checkstory.auth.firebase.toFirebaseTokenOrNull
import tech.checkstory.auth.withPrincipal

fun Application.configureUserService(userService: UserService) {
    routing {

        post<CreateUserPayload>("auth/register") {
            call.request.parseAuthorizationHeaderOrNull()?.toFirebaseTokenOrNull()?.let { token ->
                userService.create(it, token.uid!!)
                call.respond(HttpStatusCode.Created)
            }
        }

        authenticate(FIREBASE_AUTH) {
            get("auth/login") {
                withPrincipal {
                    val user = userService.read()
                    if (user != null) {
                        call.respond(HttpStatusCode.OK, UserDto.fromUser(user))
                    } else {
                        call.respond(HttpStatusCode.NotFound)
                    }
                }
            }

            post<SubscriptionInformation>("/user/subscription") {
                withPrincipal {
                    userService.markIsPaidUser(it.isPaidUser)
                    call.respond(HttpStatusCode.OK)
                }
            }

            delete("/user") {
                withPrincipal {
                    userService.delete()
                    call.respond(HttpStatusCode.OK)
                }
            }
        }
    }
}

@Serializable
class CreateUserPayload

@Serializable
data class SubscriptionInformation(val isPaidUser: Boolean)

@Serializable
data class UserDto(val userId: String, val isPaidUser: Boolean) {

    companion object {

        fun fromUser(user: User): UserDto {
            return UserDto(
                user.userId,
                user.isPaidUser
            )
        }
    }
}

class AuthenticatedScope(
    val principal: User
)
