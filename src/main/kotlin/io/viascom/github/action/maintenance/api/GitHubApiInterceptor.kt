package io.viascom.github.action.maintenance.api

import io.viascom.github.action.maintenance.core.Environment
import okhttp3.Interceptor
import okhttp3.Response

class GitHubApiInterceptor : Interceptor {

    companion object {
        // https://docs.github.com/en/rest/about-the-rest-api/api-versions?apiVersion=2022-11-28#supported-api-versions
        private const val GITHUB_API_VERSION = "2022-11-28"
        private const val USER_AGENT = "via-github-maintenance-action"
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = Environment.gitHubToken
        val request = chain.request().newBuilder()
            .header("Authorization", "Bearer $token")
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", GITHUB_API_VERSION)
            .header("User-Agent", USER_AGENT)
            .build()
        return chain.proceed(request)
    }
}
