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
            // Read user
            get("auth/login") {
                withFirebasePrincipal {
                    val user = userService.read(it.userId)
                    if (user != null) {
                        call.respond(HttpStatusCode.OK, user)
                    } else {
                        call.respond(HttpStatusCode.NotFound)
                    }
                }
            }

            // Update user
            put("/user") {
                withFirebasePrincipal {
                    val user = call.receive<User>()
                    userService.update(it.userId, user)
                    call.respond(HttpStatusCode.OK)
                }
            }

            // Delete user
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
class CreateUserPayload()
