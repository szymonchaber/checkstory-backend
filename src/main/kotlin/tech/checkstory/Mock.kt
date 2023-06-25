package tech.checkstory

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import org.jetbrains.exposed.sql.Database
import org.kodein.di.DI
import org.kodein.di.bindSingleton
import org.kodein.di.instance
import tech.checkstory.plugins.connectToPostgres
import java.util.*

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::mockModule).start(wait = true)
}

fun Application.mockModule() {
    Locale.setDefault(Locale.US)
    DI {
        import(commonDI, allowOverride = true)
        bindSingleton<Database>(overrides = true) {
            Database.connect(
                getNewConnection = {
                    connectToPostgres(true, instance())
                }
            )
        }
//        bind<EventRepository<TemplateEvent>>(overrides = true) with singleton {
//            InMemoryTemplateEventRepository()
//        }
//        bind<EventRepository<ChecklistEvent>>(overrides = true) with singleton {
//            InMemoryChecklistEventRepository()
//        }
    }.apply {
        module()
    }
}
