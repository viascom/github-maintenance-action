package io.viascom.github.action.maintenance.api

import okhttp3.Interceptor
import okhttp3.Response
import org.slf4j.LoggerFactory

/** Adds the headers GitHub requires on every request: bearer token, media type, API version and user agent. */
class GitHubApiInterceptor(private val token: String) : Interceptor {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .header("Authorization", "Bearer $token")
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", API_VERSION)
            .header("User-Agent", USER_AGENT)
            .build()
        log.debug("{} {}", request.method, request.url)
        return chain.proceed(request)
    }

    companion object {
        // https://docs.github.com/en/rest/about-the-rest-api/api-versions
        const val API_VERSION = "2022-11-28"
        const val USER_AGENT = "viascom-github-maintenance-action"
    }
}
