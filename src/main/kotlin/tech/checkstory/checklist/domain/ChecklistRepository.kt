package tech.checkstory.checklist.domain

import tech.checkstory.checklist.domain.aggregate.Checklist
import tech.checkstory.core.domain.auth.AuthenticatedScope
import java.util.*

interface ChecklistRepository {

    context(AuthenticatedScope)
    suspend fun readAll(): List<Checklist>

    context(AuthenticatedScope)
    suspend fun read(id: UUID): Checklist?
}
