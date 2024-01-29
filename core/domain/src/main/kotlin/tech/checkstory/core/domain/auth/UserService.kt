package tech.checkstory.core.domain.auth

interface UserService {

    suspend fun readById(userId: String): User?

    suspend fun create(userId: String): String

    context(AuthenticatedScope)
    suspend fun delete()
}
