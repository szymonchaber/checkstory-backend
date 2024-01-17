package tech.checkstory.template.domain

import tech.checkstory.core.domain.auth.AuthenticatedScope
import tech.checkstory.template.domain.aggregate.Template
import java.util.*

interface TemplateRepository {

    context(AuthenticatedScope)
    suspend fun read(id: UUID): Template?

    context(AuthenticatedScope)
    suspend fun readAll(): List<Template>
}
