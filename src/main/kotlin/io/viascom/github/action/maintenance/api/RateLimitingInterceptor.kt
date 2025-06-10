package io.viascom.github.action.maintenance.api

import io.viascom.github.action.maintenance.core.Environment.rateLimiter
import okhttp3.Interceptor
import okhttp3.Response
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeoutException

class RateLimitingInterceptor : Interceptor {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun intercept(chain: Interceptor.Chain): Response {
        val acquired = rateLimiter.tryConsume(1)
        if (acquired) {
            return chain.proceed(chain.request())
        } else {
            val message = "Rate limit exceeded!"
            log.error(message)
            throw TimeoutException(message)
        }
    }
}
