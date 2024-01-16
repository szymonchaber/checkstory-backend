package tech.checkstory

import io.ktor.server.application.*
import org.jetbrains.exposed.sql.Database
import org.kodein.di.DI
import org.kodein.di.bindSingleton
import org.kodein.di.instance
import tech.checkstory.plugins.connectToPostgres
import java.util.*

fun Application.mockModule() {
    Locale.setDefault(Locale.US)
    println("Starting mock module")
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
