package tech.checkstory.plugins

import io.ktor.server.application.*
import io.ktor.server.auth.*
import org.kodein.di.DI
import org.kodein.di.instance
import tech.checkstory.auth.firebase.FirebaseAuthProvider

context(DI)
fun Application.configureSecurity() {
    val firebaseAuthProvider by instance<FirebaseAuthProvider>()
    authentication {
        register(firebaseAuthProvider)
    }
}
