package tech.checkstory.template.domain

import tech.checkstory.auth.user.User
import tech.checkstory.template.domain.aggregate.Template
import java.util.*

interface TemplateRepository {

    suspend fun read(user: User, id: UUID): Template?

    suspend fun readAll(user: User): List<Template>
}