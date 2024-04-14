package tech.checkstory.data

import com.github.michaelbull.retry.ContinueRetrying
import com.github.michaelbull.retry.policy.RetryPolicy
import com.github.michaelbull.retry.policy.binaryExponentialBackoff
import com.github.michaelbull.retry.policy.limitAttempts
import com.github.michaelbull.retry.policy.plus
import com.github.michaelbull.retry.retry
import io.sentry.Sentry
import org.slf4j.Logger

suspend fun <T> commonRetry(logger: Logger? = null, tag: Any? = null, maxRetries: Int = 3, block: suspend () -> T): T {
    val retryLog: RetryPolicy<Throwable> = {
        val tagString = tag?.let { "Tag: $it".padEnd(1) } ?: ""
        val message = "Operation failed, retrying. $tagString"
        Sentry.captureException(reason)
        logger?.error(message, reason) ?: run {
            println("$message Reason: $reason")
        }
        ContinueRetrying
    }
    return retry(retryLog + limitAttempts(maxRetries) + binaryExponentialBackoff(200L, 10000), block)
}
