package tech.checkstory.checklist

import io.ktor.server.application.*
import org.kodein.di.DI

context(DI)
fun Application.configureChecklist() {
    configureChecklistService()
}
