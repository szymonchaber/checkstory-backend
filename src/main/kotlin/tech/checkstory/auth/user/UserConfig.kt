package tech.checkstory.auth.user

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import tech.checkstory.auth.firebase.FIREBASE_AUTH
import tech.checkstory.auth.firebase.parseAuthorizationHeaderOrNull
import tech.checkstory.auth.firebase.toFirebaseTokenOrNull
import tech.checkstory.auth.withFirebasePrincipal

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
                withFirebasePrincipal {
                    val user = userService.read(it.userId)
                    if (user != null) {
                        call.respond(HttpStatusCode.OK, UserDto.fromUser(user))
                    } else {
                        call.respond(HttpStatusCode.NotFound)
                    }
                }
            }

            put("/user") {
                withFirebasePrincipal {
                    val user = call.receive<User>()
                    userService.update(it.userId, user)
                    call.respond(HttpStatusCode.OK)
                }
            }

            post<SubscriptionInformation>("/user/subscription") {
                withFirebasePrincipal { user ->
                    userService.markIsPaidUser(user, it.isPaidUser)
                    call.respond(HttpStatusCode.OK)
                }
            }

            delete("/user") {
                withFirebasePrincipal {
                    userService.delete(it.userId)
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