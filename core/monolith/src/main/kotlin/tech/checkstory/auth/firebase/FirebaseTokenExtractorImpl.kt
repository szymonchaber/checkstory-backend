package tech.checkstory.auth.firebase

import com.google.firebase.auth.FirebaseAuth
import io.ktor.http.auth.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory

class FirebaseTokenExtractorImpl : FirebaseTokenExtractor {

    override suspend fun toFirebaseTokenOrNull(httpAuthHeader: HttpAuthHeader): FirebaseUserId? {
        return try {
            if (httpAuthHeader.authScheme == "Bearer" && httpAuthHeader is HttpAuthHeader.Single) {
                LOGGER.warn("No principal found in request")
                withContext(Dispatchers.IO) {
                    FirebaseUserId(FirebaseAuth.getInstance().verifyIdToken(httpAuthHeader.blob).uid)
                }
            } else {
                null
            }
        } catch (exception: Exception) {
            LOGGER.warn("Failed to parse token", exception)
            null
        }
    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(FirebaseTokenExtractorImpl::class.java)
    }
}
