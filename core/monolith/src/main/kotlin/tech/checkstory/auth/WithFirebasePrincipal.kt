package tech.checkstory.auth

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.util.pipeline.*
import tech.checkstory.core.domain.auth.AuthenticatedScope
import tech.checkstory.core.domain.auth.User

suspend fun PipelineContext<Unit, ApplicationCall>.withPrincipal(
    block: suspend context(AuthenticatedScope) () -> Unit
) {
    call.principal<User>()
        ?.let {
            block(AuthenticatedScope(it))
        } ?: call.respond(HttpStatusCode.Unauthorized)
}
