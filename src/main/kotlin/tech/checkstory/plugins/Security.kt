package tech.checkstory.plugins

import io.ktor.server.application.*
import io.ktor.server.auth.*
import tech.checkstory.auth.firebase.firebase
import tech.checkstory.auth.user.UserService

fun Application.configureSecurity(userService: UserService) {
    authentication {
        firebase {
            validate {
                userService.readById(it.uid) ?: error("Provided user token is not a registered user")
            }
        }
    }
}
