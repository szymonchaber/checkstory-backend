package tech.checkstory.auth.module

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
import tech.checkstory.core.domain.auth.DeleteUserUseCase
import tech.checkstory.core.domain.auth.User
import tech.checkstory.core.domain.auth.UserService

context(DI)
fun Application.configureUserService() {
    log.debug("Got started from auth module!")
    val userService by instance<UserService>()
    val deleteUserUseCase by instance<DeleteUserUseCase>()
    routing {
        post("auth/register") {
            call.request.parseAuthorizationHeaderOrNull()?.toFirebaseTokenOrNull()?.let { token ->
                userService.create(token.uid!!)
                call.respond(HttpStatusCode.Created, UserDto(token.uid, false))
            }
        }

        authenticate(FIREBASE_AUTH) {
            get("auth/login") {
                withPrincipal {
                    call.respond(HttpStatusCode.OK, UserDto.fromUser(principal))
                }
            }

            post("auth/delete") {
                withPrincipal {
                    deleteUserUseCase.deleteUser()
                    call.respond(HttpStatusCode.OK)
                }
            }
        }
    }
}

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
