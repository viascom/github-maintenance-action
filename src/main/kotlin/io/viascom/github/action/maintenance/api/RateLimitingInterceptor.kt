    package io.viascom.github.action.maintenance.api

    import okhttp3.Interceptor
    import okhttp3.Response
    import org.slf4j.LoggerFactory
    import java.time.Duration
    import java.time.LocalDateTime
    import java.util.concurrent.atomic.AtomicInteger
    import kotlin.system.exitProcess

    class RateLimitingInterceptor : Interceptor {

        private val log = LoggerFactory.getLogger(javaClass)

        private val requestLimit = 1_000
        private val requestCounter = AtomicInteger(0)
        private var resetTime = LocalDateTime.now().plusHours(1)

        override fun intercept(chain: Interceptor.Chain): Response {
            synchronized(this) {
                val now = LocalDateTime.now()
                if (now.isAfter(resetTime)) {
                    resetTime = now.plusHours(1)
                    requestCounter.set(0)
                }

                if (requestCounter.incrementAndGet() > requestLimit) {
                    val delay = Duration.between(now, resetTime).toMillis()
                    log.info("Rate limit hit for GitHub API requests. Discarding maintenance run!")
                    exitProcess(1)
                }
            }

            return chain.proceed(chain.request())
        }
    }