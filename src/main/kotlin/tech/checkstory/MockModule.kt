package tech.checkstory

import io.ktor.server.application.*
import org.jetbrains.exposed.sql.Database
import org.kodein.di.*
import tech.checkstory.checklist.data.InMemoryChecklistEventRepository
import tech.checkstory.checklist.domain.event.ChecklistEvent
import tech.checkstory.data.EventRepository
import tech.checkstory.plugins.connectToPostgres
import tech.checkstory.read.subscription.SubscriptionToAll
import tech.checkstory.template.data.InMemoryTemplateEventRepository
import tech.checkstory.template.domain.event.TemplateEvent
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
    }
}
