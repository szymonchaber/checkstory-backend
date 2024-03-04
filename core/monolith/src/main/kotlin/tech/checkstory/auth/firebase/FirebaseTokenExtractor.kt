package tech.checkstory.auth.firebase

import io.ktor.http.auth.*

interface FirebaseTokenExtractor {

    suspend fun toFirebaseTokenOrNull(httpAuthHeader: HttpAuthHeader): FirebaseUserId?
}
