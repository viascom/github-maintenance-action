package io.viascom.github.action.maintenance.api

import okhttp3.Interceptor
import okhttp3.Response
import org.slf4j.LoggerFactory
import java.time.Clock
import java.time.Duration
import java.time.Instant

/**
 * Applies GitHub's documented handling of primary and secondary rate limits: a 403 or 429
 * that carries `retry-after` is retried after that many seconds, one with
 * `x-ratelimit-remaining: 0` after the `x-ratelimit-reset` instant, and a secondary-limit
 * response without either header after a minute that doubles on every further attempt.
 * See https://docs.github.com/en/rest/using-the-rest-api/best-practices-for-using-the-rest-api
 */
class RateLimitInterceptor(private val clock: Clock = Clock.systemUTC(), private val sleep: (Duration) -> Unit = { Thread.sleep(it) }, private val maxRetries: Int = 3) :
    Interceptor {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        var response = chain.proceed(request)
        var attempt = 0
        while (attempt < maxRetries && response.isRateLimited()) {
            val wait = response.rateLimitWait(attempt)
            response.close()
            attempt++
            log.warn("GitHub rate limit reached for {} {}; waiting {}s before retry {}/{}", request.method, request.url, wait.seconds, attempt, maxRetries)
            sleep(wait)
            response = chain.proceed(request)
        }
        return response
    }

    private fun Response.isRateLimited(): Boolean {
        if (code != 403 && code != 429) return false
        if (header("retry-after") != null || header("x-ratelimit-remaining") == "0") return true
        return peekBody(PEEK_BYTES).string().contains("secondary rate limit", ignoreCase = true)
    }

    private fun Response.rateLimitWait(attempt: Int): Duration {
        header("retry-after")?.toLongOrNull()?.let { return Duration.ofSeconds(it).coerceIn(MIN_WAIT, MAX_WAIT) }
        if (header("x-ratelimit-remaining") == "0") {
            header("x-ratelimit-reset")?.toLongOrNull()?.let { reset ->
                return Duration.between(clock.instant(), Instant.ofEpochSecond(reset)).coerceIn(MIN_WAIT, MAX_WAIT)
            }
        }
        return SECONDARY_LIMIT_BASE_WAIT.multipliedBy(1L shl attempt)
    }

    private companion object {
        const val PEEK_BYTES = 2048L
        val MIN_WAIT: Duration = Duration.ofSeconds(1)
        val MAX_WAIT: Duration = Duration.ofHours(1)
        val SECONDARY_LIMIT_BASE_WAIT: Duration = Duration.ofMinutes(1)
    }
}
