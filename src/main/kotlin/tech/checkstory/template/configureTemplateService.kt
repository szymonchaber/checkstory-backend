package tech.checkstory.template

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.Database
import tech.checkstory.auth.withFirebasePrincipal
import tech.checkstory.firebase.FIREBASE_AUTH

fun Application.configureTemplateService() {

    val database = Database.connect(
        url = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1",
        user = "root",
        driver = "org.h2.Driver",
        password = ""
    )
    val templateService = TemplateService(database)
    routing {
        authenticate(FIREBASE_AUTH) {

            // Create user
            post("/templates") {
                withFirebasePrincipal {
                    val user = call.receive<Template>()
                    val id = templateService.create(user, it)
                    call.respond(HttpStatusCode.Created, id)
                }
            }

            // Read user
            get("/templates") {
                withFirebasePrincipal {
                    val templates = templateService.read(it)
                    call.respond(HttpStatusCode.OK, templates)
                }
            }

            // Read user
            get("/templates/{id}") {
                withFirebasePrincipal {
                    val id = call.parameters["id"]?.toInt() ?: throw IllegalArgumentException("Invalid ID")
                    val template = templateService.read(it, id)
                    if (template != null) {
                        call.respond(HttpStatusCode.OK, template)
                    } else {
                        call.respond(HttpStatusCode.NotFound)
                    }
                }
            }

            // Update user
//            put("/user") {
//                withFirebasePrincipal {
//                    val user = call.receive<Template>()
//                    templateService.update(it.userId, user)
//                    call.respond(HttpStatusCode.OK)
//                }
//            }

            // Delete user
            delete("/templates/{id}") {
                withFirebasePrincipal {
                    val id = call.parameters["id"]?.toInt() ?: throw IllegalArgumentException("Invalid ID")
                    templateService.delete(it, id)
                    call.respond(HttpStatusCode.OK)
                }
            }
        }
    }
}
