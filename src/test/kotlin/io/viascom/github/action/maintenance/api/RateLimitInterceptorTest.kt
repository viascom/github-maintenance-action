package io.viascom.github.action.maintenance.api

import ch.tutteli.atrium.api.infix.en_GB.toEqual
import ch.tutteli.atrium.api.verbs.expect
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.junit5.StartStop
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset

class RateLimitInterceptorTest {

    @StartStop
    private val server = MockWebServer()

    private val now: Instant = Instant.parse("2026-10-19T10:00:00Z")
    private val sleeps = mutableListOf<Duration>()
    private val client = OkHttpClient.Builder()
        .addInterceptor(RateLimitInterceptor(Clock.fixed(now, ZoneOffset.UTC), sleep = { sleeps += it }, maxRetries = 3))
        .build()

    private fun call(): Int = client.newCall(Request.Builder().url(server.url("/rate")).build()).execute().use { it.code }

    private fun limited(code: Int, vararg headers: Pair<String, String>, body: String = "") =
        MockResponse.Builder().code(code).body(body).apply { headers.forEach { (name, value) -> addHeader(name, value) } }.build()

    private val ok = MockResponse.Builder().code(200).build()

    /** Requirement pinned: retry-after is honoured to the second, with a floor of one second. */
    @Test
    fun `waits the retry-after seconds`() {
        server.enqueue(limited(403, "retry-after" to "30"))
        server.enqueue(limited(429, "retry-after" to "0"))
        server.enqueue(ok)

        expect(call()) toEqual 200
        expect(sleeps.toList()) toEqual listOf(Duration.ofSeconds(30), Duration.ofSeconds(1))
    }

    /** Requirement pinned: an exhausted primary limit waits until x-ratelimit-reset, never longer than an hour. */
    @Test
    fun `waits until the primary limit resets`() {
        server.enqueue(limited(403, "x-ratelimit-remaining" to "0", "x-ratelimit-reset" to now.plusSeconds(600).epochSecond.toString()))
        server.enqueue(limited(403, "x-ratelimit-remaining" to "0", "x-ratelimit-reset" to now.plusSeconds(99_999).epochSecond.toString()))
        server.enqueue(ok)

        expect(call()) toEqual 200
        expect(sleeps.toList()) toEqual listOf(Duration.ofSeconds(600), Duration.ofHours(1))
    }

    /** Requirement pinned: a secondary limit without headers backs off a minute, then doubles, and gives up after the retries. */
    @Test
    fun `backs off exponentially on a secondary limit and gives up`() {
        repeat(4) { server.enqueue(limited(403, body = """{"message":"You have exceeded a secondary rate limit."}""")) }

        expect(call()) toEqual 403
        expect(server.requestCount) toEqual 4
        expect(sleeps.toList()) toEqual listOf(Duration.ofMinutes(1), Duration.ofMinutes(2), Duration.ofMinutes(4))
    }

    /** Requirement pinned: a 403 that is not a rate limit, such as a missing permission, is returned untouched. */
    @Test
    fun `does not retry an ordinary forbidden answer`() {
        server.enqueue(limited(403, "x-ratelimit-remaining" to "999", body = """{"message":"Resource not accessible by integration"}"""))

        expect(call()) toEqual 403
        expect(server.requestCount) toEqual 1
        expect(sleeps.toList()) toEqual emptyList()
    }

    /** Requirement pinned: a successful answer passes straight through. */
    @Test
    fun `passes a successful answer through`() {
        server.enqueue(ok)

        expect(call()) toEqual 200
        expect(sleeps.toList()) toEqual emptyList()
    }
}
