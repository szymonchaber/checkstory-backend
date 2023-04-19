package tech.checkstory.template

import tech.checkstory.firebase.user.FirebaseUser
import java.util.*

interface TemplateRepository {

    suspend fun read(firebaseUser: FirebaseUser, id: UUID): Template?

    suspend fun readAll(firebaseUser: FirebaseUser): List<Template>
}