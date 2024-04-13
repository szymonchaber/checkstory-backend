package tech.checkstory.auth.firebase

import io.ktor.http.auth.*
import io.ktor.server.auth.*
import io.ktor.server.request.*


fun ApplicationRequest.parseAuthorizationHeaderOrNull(): HttpAuthHeader? {
    return try {
        parseAuthorizationHeader()
    } catch (exception: Exception) {
        println("failed to parse token")
        null
    }
}

@JvmInline
value class FirebaseUserId(val uid: String)
