package tech.checkstory.core.domain.auth

interface UserService {

    suspend fun readById(userId: String): User?

    suspend fun create(userId: String, email: String?): String

    context(AuthenticatedScope)
    suspend fun delete()
}
