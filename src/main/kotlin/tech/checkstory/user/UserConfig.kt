package tech.checkstory.user

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.Database
import tech.checkstory.auth.withFirebasePrincipal
import tech.checkstory.firebase.FIREBASE_AUTH

fun Application.configureUserService() {

    val database = Database.connect(
        url = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1",
        user = "root",
        driver = "org.h2.Driver",
        password = ""
    )
    val userService = UserService(database)
    routing {
        authenticate(FIREBASE_AUTH) {

            // Create user
            post("/user") {
                withFirebasePrincipal {
                    val user = call.receive<User>()
                    val id = userService.create(user, it)
                    call.respond(HttpStatusCode.Created, id)
                }
            }

            // Read user
            get("/user") {
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
