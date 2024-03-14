package tech.checkstory.template.module

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.kodein.di.DI
import org.kodein.di.instance
import tech.checkstory.auth.firebase.FIREBASE_AUTH
import tech.checkstory.auth.withPrincipal
import tech.checkstory.template.data.TemplateRepository
import tech.checkstory.template.domain.aggregate.Template

context(DI)
fun Application.configureTemplate() {
    val templateService by instance<TemplateRepository>()
    routing {
        authenticate(FIREBASE_AUTH) {
            get("/templates") {
                withPrincipal {
                    val templates = templateService.readAllNotDeleted()
                    call.respond<List<Template>>(HttpStatusCode.OK, templates)
                }
            }
        }
    }
}
