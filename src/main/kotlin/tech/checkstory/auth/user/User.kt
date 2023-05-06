package tech.checkstory.auth.user

import io.ktor.server.auth.*

data class User(val userId: String) : Principal
