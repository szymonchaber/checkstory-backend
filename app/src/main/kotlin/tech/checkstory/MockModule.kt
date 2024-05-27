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
import java.util.*

fun Application.mockModule(operations: DI.() -> Unit = {}, database: Database = mockDatabase()) {
    Locale.setDefault(Locale.US)
    println("Starting mock module")
    DI {
        import(commonDI, allowOverride = true)
        bindSingleton<Database>(overrides = true) {
            database
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
        routingV1 {
            get("/") {
                call.respondText("Hello, mock!")
            }
        }
        operations()
    }
}

fun mockDatabase(name: String = "test") = Database.connect(
//       "jdbc:h2:~/h2db;AUTO_SERVER=TRUE;DB_CLOSE_DELAY=-1;MODE=MYSQL;DATABASE_TO_UPPER=false",
    "jdbc:h2:mem:$name;DB_CLOSE_DELAY=-1;MODE=MYSQL;DATABASE_TO_UPPER=false",
    user = "root",
    password = "",
    driver = "org.h2.Driver"
)
