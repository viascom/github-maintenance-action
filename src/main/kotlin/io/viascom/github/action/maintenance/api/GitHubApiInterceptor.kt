package io.viascom.github.action.maintenance.api

import io.viascom.github.action.maintenance.core.Environment
import okhttp3.Interceptor
import okhttp3.Response

class GitHubApiInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .header("Authorization", "Bearer ${Environment.gitHubToken}")
            .header("Accept", "application/vnd.github+json")
            .build()

        return chain.proceed(request)
    }
}