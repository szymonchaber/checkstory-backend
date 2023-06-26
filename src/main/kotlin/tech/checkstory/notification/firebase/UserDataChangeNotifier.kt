package tech.checkstory.notification.firebase

import com.google.firebase.messaging.*
import io.ktor.util.logging.*
import tech.checkstory.notification.data.FirebaseMessagingTokenRepository
import tech.checkstory.notification.domain.FirebaseMessagingToken

class UserDataChangeNotifier(
    private val messagingTokenRepository: FirebaseMessagingTokenRepository,
    private val logger: Logger,
) {

    private val firebaseMessaging: FirebaseMessaging by lazy {
        FirebaseMessaging.getInstance()
    }

    suspend fun notifyUserOfDataChange(userId: String) {
        logger.debug("Notifying the user of data changes")
        notifyUser(messagingTokenRepository.readAllById(userId))
    }

    suspend fun notifyUserOfPaymentStatusChange(userId: String) {
        logger.debug("Notifying the user of payment changes")
        notifyUser(messagingTokenRepository.readAllById(userId))
    }

    private suspend fun notifyUser(tokens: List<FirebaseMessagingToken>) {
        if (tokens.isEmpty()) {
            return
        }
        tokens.mapNotNull { token ->
            try {
                Message.builder()
                    .setToken(token.token)
                    .setNotification(
                        Notification.builder()
                            .setTitle("I might just have sent this to 400 people")
                            .setBody("But maybe just 1")
                            .build()
                    )
                    .build() to token
            } catch (illegalArgumentException: IllegalArgumentException) {
                null
            }
        }
            .unzip()
            .let { (messages, tokens) ->
                try {
                    val results = firebaseMessaging.sendAll(messages)
                    handleResponses(tokens, results)
                } catch (firebaseException: FirebaseMessagingException) {
                    println(firebaseException.messagingErrorCode)
                }
            }
    }

    private suspend fun handleResponses(tokens: List<FirebaseMessagingToken>, sendAll: BatchResponse) {
        tokens.zip(sendAll.responses)
            .forEach { (token, response) ->
                if (isTokenNoLongerValid(response)) {
                    logger.info("Removing invalid firebase messaging token")
                    messagingTokenRepository.remove(token)
                }
            }
    }

    private fun isTokenNoLongerValid(response: SendResponse): Boolean {
        return response.exception != null && response.exception.messagingErrorCode == MessagingErrorCode.UNREGISTERED
    }
}
