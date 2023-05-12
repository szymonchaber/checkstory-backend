package tech.checkstory.checklist.domain

import tech.checkstory.auth.user.AuthenticatedScope
import tech.checkstory.checklist.domain.aggregate.Checklist
import java.util.*

interface ChecklistRepository {

    context(AuthenticatedScope)
    suspend fun readAll(): List<Checklist>

    context(AuthenticatedScope)
    suspend fun read(id: UUID): Checklist?
}