package tech.checkstory.core.domain.auth

interface SaveAccountDeleteRequestUseCase {

    suspend fun saveAccountDeleteRequest(email: String)
}
