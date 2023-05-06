package tech.checkstory.auth

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.util.pipeline.*
import tech.checkstory.auth.user.User

suspend fun PipelineContext<Unit, ApplicationCall>.withFirebasePrincipal(
    block: suspend PipelineContext<Unit, ApplicationCall>.(User) -> Unit
) {
    call.principal<User>()
        ?.let {
            block(it)
        } ?: call.respond(HttpStatusCode.Unauthorized)
}