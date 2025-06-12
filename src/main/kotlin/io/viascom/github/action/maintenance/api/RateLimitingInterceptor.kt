package io.viascom.github.action.maintenance.api

import io.viascom.github.action.maintenance.core.Environment.rateLimiter
import io.viascom.github.action.maintenance.exception.RateLimitExceededException
import okhttp3.Interceptor
import okhttp3.Response
import org.slf4j.LoggerFactory

class RateLimitingInterceptor : Interceptor {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val requestInfo = "${request.method} ${request.url}"

        val acquired = rateLimiter.tryConsume(1)
        return if (acquired) {
            chain.proceed(request)
        } else {
            val message = "Rate limit exceeded for request: $requestInfo!"
            log.error(message)
            throw RateLimitExceededException(message)
        }
    }
}
