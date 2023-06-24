package tech.checkstory

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import org.jetbrains.exposed.sql.Database
import org.kodein.di.*
import tech.checkstory.checklist.data.InMemoryChecklistEventRepository
import tech.checkstory.checklist.domain.event.ChecklistEvent
import tech.checkstory.data.EventRepository
import tech.checkstory.plugins.connectToPostgres
import tech.checkstory.template.data.InMemoryTemplateEventRepository
import tech.checkstory.template.domain.event.TemplateEvent
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
        bind<EventRepository<TemplateEvent>>(overrides = true) with singleton {
            InMemoryTemplateEventRepository()
        }
        bind<EventRepository<ChecklistEvent>>(overrides = true) with singleton {
            InMemoryChecklistEventRepository()
        }
    }.apply {
        module()
    }
}
