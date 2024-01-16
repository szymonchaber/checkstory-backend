package tech.checkstory.core.domain.auth

import io.ktor.server.auth.*

data class User(val userId: String, val isPaidUser: Boolean = false) : Principal
