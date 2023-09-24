package tech.checkstory.template.data

import com.github.michaelbull.retry.ContinueRetrying
import com.github.michaelbull.retry.policy.RetryPolicy
import com.github.michaelbull.retry.policy.binaryExponentialBackoff
import com.github.michaelbull.retry.policy.limitAttempts
import com.github.michaelbull.retry.policy.plus
import com.github.michaelbull.retry.retry

suspend fun <T> commonRetry(tag: Any? = null, block: suspend () -> T): T {
    val retryLog: RetryPolicy<Throwable> = {
        val tagString = tag?.let { "Tag: $it".padEnd(1) } ?: ""
        println("Operation failed, retrying. ${tagString}Reason: $reason")
        ContinueRetrying
    }
    return retry(retryLog + limitAttempts(50) + binaryExponentialBackoff(200L, 10000), block)
}
