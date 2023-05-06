package tech.checkstory.checklist.domain

import tech.checkstory.auth.user.User
import tech.checkstory.checklist.domain.aggregate.Checklist
import java.util.*

interface ChecklistRepository {

    suspend fun readAll(user: User): List<Checklist>
    suspend fun read(user: User, id: UUID): Checklist?
}