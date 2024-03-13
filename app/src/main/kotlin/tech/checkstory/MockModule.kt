package tech.checkstory

import io.ktor.http.auth.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.Database
import org.kodein.di.DI
import org.kodein.di.bindSingleton
import tech.checkstory.auth.firebase.FirebaseTokenExtractor
import tech.checkstory.auth.firebase.FirebaseUserId
import java.sql.DriverManager
import java.util.*

fun Application.mockModule(operations: (DI.() -> Unit) = {}) {
    Locale.setDefault(Locale.US)
    println("Starting mock module")
    DI {
        import(commonDI, allowOverride = true)
        bindSingleton<Database>(overrides = true) {
            Database.connect(
                getNewConnection = {
                    DriverManager.getConnection(
//                        "jdbc:h2:~/h2db;AUTO_SERVER=TRUE;DB_CLOSE_DELAY=-1;MODE=MYSQL;DATABASE_TO_UPPER=false",
                        "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;MODE=MYSQL;DATABASE_TO_UPPER=false",
                        "root",
                        ""
                    )
                }
            )
        }
        bindSingleton<FirebaseTokenExtractor>(overrides = true) {
            object : FirebaseTokenExtractor {

                override suspend fun toFirebaseTokenOrNull(httpAuthHeader: HttpAuthHeader): FirebaseUserId {
                    return FirebaseUserId("mock-user-id")
                }
            }
        }
    }.apply {
        module()
        routing {
            get("/") {
                call.respondText("Hello, mock!")
            }
        }
        operations()
    }
}
