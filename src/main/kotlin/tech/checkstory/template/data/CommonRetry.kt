package tech.checkstory.template.data

import com.github.michaelbull.retry.ContinueRetrying
import com.github.michaelbull.retry.policy.RetryPolicy
import com.github.michaelbull.retry.policy.constantDelay
import com.github.michaelbull.retry.policy.limitAttempts
import com.github.michaelbull.retry.policy.plus
import com.github.michaelbull.retry.retry

suspend fun <T> commonRetry(block: suspend () -> T): T {
    require(50L > 0) { "delayMillis must be positive: ${50L}" }
    val retryLog: RetryPolicy<Throwable> = {
        println("Operation failed, retrying. Reason: $reason")
        ContinueRetrying
    }
    return retry(retryLog + limitAttempts(4) + constantDelay(delayMillis = 50L), block)
}
