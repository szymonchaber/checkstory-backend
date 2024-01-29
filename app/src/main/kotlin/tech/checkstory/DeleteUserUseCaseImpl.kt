package tech.checkstory

import tech.checkstory.core.domain.auth.AuthenticatedScope
import tech.checkstory.core.domain.auth.DeleteUserUseCase
import tech.checkstory.core.domain.auth.UserService

class DeleteUserUseCaseImpl(private val userService: UserService) : DeleteUserUseCase {

    context(AuthenticatedScope)
    override suspend fun deleteUser() {
        userService.delete()
    }
}
