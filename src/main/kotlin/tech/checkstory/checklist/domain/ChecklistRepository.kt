package tech.checkstory.checklist.domain

import tech.checkstory.checklist.domain.aggregate.Checklist
import tech.checkstory.firebase.user.FirebaseUser
import java.util.*

interface ChecklistRepository {

    suspend fun readAll(firebaseUser: FirebaseUser): List<Checklist>
    suspend fun read(firebaseUser: FirebaseUser, id: UUID): Checklist?
}