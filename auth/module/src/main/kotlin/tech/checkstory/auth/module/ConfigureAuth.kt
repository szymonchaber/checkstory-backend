package tech.checkstory.auth.module

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.kodein.di.DI
import org.kodein.di.instance
import tech.checkstory.auth.firebase.FirebaseTokenExtractor
import tech.checkstory.auth.firebase.authenticateFirebase
import tech.checkstory.auth.firebase.parseAuthorizationHeaderOrNull
import tech.checkstory.auth.withPrincipal
import tech.checkstory.core.domain.auth.DeleteUserUseCase
import tech.checkstory.core.domain.auth.SaveAccountDeleteRequestUseCase
import tech.checkstory.core.domain.auth.User
import tech.checkstory.core.domain.auth.UserService

context(DI)
fun Application.configureAuth() {
    val userService by instance<UserService>()
    val deleteUserUseCase by instance<DeleteUserUseCase>()
    val firebaseTokenExtractor by instance<FirebaseTokenExtractor>()
    val saveAccountDeleteRequestUseCase by instance<SaveAccountDeleteRequestUseCase>()
    routing {
        route("auth") {
            post<UserCreationPayloadDto>("register") { payload ->
                try {
                    val parseAuthorizationHeaderOrNull = call.request.parseAuthorizationHeaderOrNull()
                    parseAuthorizationHeaderOrNull?.let {
                        firebaseTokenExtractor.toFirebaseTokenOrNull(it)
                    }?.let {
                        userService.create(it.uid, payload.email?.ifEmpty { null })
                        call.respond(HttpStatusCode.Created, UserDto(it.uid, payload.email, false))
                    } ?: call.respond(HttpStatusCode.Unauthorized)
                } catch (exception: Exception) {
                    this@configureAuth.log.error("Failed to register user", exception)
                    call.respond(HttpStatusCode.InternalServerError)
                }
            }

            authenticateFirebase {
                get("login") {
                    withPrincipal {
                        call.respond(HttpStatusCode.OK, UserDto.fromUser(principal))
                    }
                }

                post("delete") {
                    withPrincipal {
                        deleteUserUseCase.deleteUser()
                        call.respond(HttpStatusCode.OK)
                    }
                }
            }
        }
        post<DeleteAccountFormRequestPayloadDto>("delete-account-form") {
            if (it.email.isEmpty() || it.email.length > 254) {
                call.respond(HttpStatusCode.BadRequest)
                return@post
            }
            saveAccountDeleteRequestUseCase.saveAccountDeleteRequest(it.email)
            call.respond(HttpStatusCode.OK)
        }
    }
}

@Serializable
data class UserDto(
    val userId: String,
    val email: String?,
    val isPaidUser: Boolean
) {

    companion object {

        fun fromUser(user: User): UserDto {
            return UserDto(
                user.userId,
                user.email,
                user.isPaidUser,
            )
        }
    }
}

@Serializable
data class UserCreationPayloadDto(
    val email: String?
)

@Serializable
data class DeleteAccountFormRequestPayloadDto(
    val email: String
)
