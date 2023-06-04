package tech.checkstory.auth.user

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.kodein.di.DI
import org.kodein.di.instance
import tech.checkstory.auth.firebase.FIREBASE_AUTH
import tech.checkstory.auth.firebase.parseAuthorizationHeaderOrNull
import tech.checkstory.auth.firebase.toFirebaseTokenOrNull
import tech.checkstory.auth.withPrincipal

context(DI)
fun Application.configureUserService() {
    val userService by instance<UserService>()
    routing {

        post<CreateUserPayload>("auth/register") {
            call.request.parseAuthorizationHeaderOrNull()?.toFirebaseTokenOrNull()?.let { token ->
                userService.create(it, token.uid!!)
                call.respond(HttpStatusCode.Created, UserDto(token.uid, false))
            }
        }

        authenticate(FIREBASE_AUTH) {
            get("auth/login") {
                withPrincipal {
                    call.respond(HttpStatusCode.OK, UserDto.fromUser(principal))
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
