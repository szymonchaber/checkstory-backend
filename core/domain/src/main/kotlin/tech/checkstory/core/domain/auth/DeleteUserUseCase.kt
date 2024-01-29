package tech.checkstory.core.domain.auth

interface DeleteUserUseCase {

    context(AuthenticatedScope)
    suspend fun deleteUser()
}
