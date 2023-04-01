package tech.checkstory.auth

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.util.pipeline.*
import tech.checkstory.firebase.user.FirebaseUser

suspend fun PipelineContext<Unit, ApplicationCall>.withFirebasePrincipal(block: suspend PipelineContext<Unit, ApplicationCall>.(FirebaseUser) -> Unit) {
    call.principal<FirebaseUser>()?.let {
        block(it)
    }
}