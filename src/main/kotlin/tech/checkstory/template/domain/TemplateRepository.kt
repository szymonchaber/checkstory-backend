package tech.checkstory.template.domain

import tech.checkstory.auth.user.AuthenticatedScope
import java.util.*

interface TemplateRepository {

    context(AuthenticatedScope)
    suspend fun read(id: UUID): Template?

    context(AuthenticatedScope)
    suspend fun readAll(): List<Template>
}
