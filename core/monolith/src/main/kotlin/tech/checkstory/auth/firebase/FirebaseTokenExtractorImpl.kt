package tech.checkstory.auth.firebase

import com.google.firebase.auth.FirebaseAuth
import io.ktor.http.auth.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FirebaseTokenExtractorImpl : FirebaseTokenExtractor {

    override suspend fun toFirebaseTokenOrNull(httpAuthHeader: HttpAuthHeader): FirebaseUserId? {
        return try {
            if (httpAuthHeader.authScheme == "Bearer" && httpAuthHeader is HttpAuthHeader.Single) {
                withContext(Dispatchers.IO) {
                    FirebaseUserId(FirebaseAuth.getInstance().verifyIdToken(httpAuthHeader.blob).uid)
                }
            } else {
                null
            }
        } catch (exception: Exception) {
            exception.printStackTrace()
            null
        }
    }
}
