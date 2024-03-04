package tech.checkstory

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.Database
import org.kodein.di.DI
import org.kodein.di.bind
import org.kodein.di.bindSingleton
import org.kodein.di.singleton
import tech.checkstory.checklist.data.InMemoryChecklistEventRepository
import tech.checkstory.checklist.domain.event.ChecklistEvent
import tech.checkstory.data.EventRepository
import tech.checkstory.read.subscription.SubscriptionToAll
import tech.checkstory.template.data.InMemoryTemplateEventRepository
import tech.checkstory.template.domain.event.TemplateEvent
import java.sql.DriverManager
import java.util.*

fun Application.mockModule() {
    Locale.setDefault(Locale.US)
    println("Starting mock module")
    DI {
        import(commonDI, allowOverride = true)
        bindSingleton<Database>(overrides = true) {
            Database.connect(
                getNewConnection = {
                    DriverManager.getConnection(
                        "jdbc:h2:~/h2db;AUTO_SERVER=TRUE;DB_CLOSE_DELAY=-1;MODE=MYSQL",
                        "root",
                        ""
                    )
                }
            )
        }
        bind<EventRepository<TemplateEvent>>(overrides = true) with singleton {
            InMemoryTemplateEventRepository()
        }
        bind<EventRepository<ChecklistEvent>>(overrides = true) with singleton {
            InMemoryChecklistEventRepository()
        }
        bind<SubscriptionToAll>(overrides = true) with singleton {
            object : SubscriptionToAll {

                override suspend fun subscribeToAll() {
                    // nop
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
    }
}
