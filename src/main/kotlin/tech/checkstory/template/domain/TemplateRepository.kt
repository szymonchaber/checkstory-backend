package tech.checkstory.template.domain

import tech.checkstory.firebase.user.FirebaseUser
import tech.checkstory.template.domain.aggregate.Template
import java.util.*

interface TemplateRepository {

    suspend fun read(firebaseUser: FirebaseUser, id: UUID): Template?

    suspend fun readAll(firebaseUser: FirebaseUser): List<Template>
}