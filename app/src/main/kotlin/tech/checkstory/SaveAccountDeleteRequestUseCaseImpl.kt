package tech.checkstory

import tech.checkstory.auth.data.AccountDeletionRequestRepository
import tech.checkstory.core.domain.auth.SaveAccountDeleteRequestUseCase

class SaveAccountDeleteRequestUseCaseImpl(
    private val repository: AccountDeletionRequestRepository
) : SaveAccountDeleteRequestUseCase {

    override suspend fun saveAccountDeleteRequest(email: String) {
        repository.save(email)
    }
}
