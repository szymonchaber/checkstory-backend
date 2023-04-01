package tech.checkstory.plugins

import io.ktor.server.application.*
import io.ktor.server.auth.*
import tech.checkstory.firebase.firebase
import tech.checkstory.firebase.user.FirebaseUser

fun Application.configureSecurity() {
    authentication {
        firebase {
            validate {
                // TODO look up user profile from DB
                FirebaseUser(it.uid, it.name.orEmpty())
            }
        }
    }
}
