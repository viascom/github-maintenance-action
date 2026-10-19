package io.viascom.github.action.maintenance.api

import com.google.gson.Gson
import io.viascom.github.action.maintenance.model.Artifact
import io.viascom.github.action.maintenance.model.WorkflowArtifacts
import io.viascom.github.action.maintenance.model.WorkflowRun
import io.viascom.github.action.maintenance.model.WorkflowRuns
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.time.Duration

/**
 * The slice of the GitHub REST API this action needs, scoped to one repository.
 * https://docs.github.com/en/rest/actions/workflow-runs and
 * https://docs.github.com/en/rest/actions/artifacts
 */
class GitHubApi(
    baseUrl: String,
    token: String,
    private val owner: String,
    private val repo: String,
    client: OkHttpClient = OkHttpClient(),
    rateLimitInterceptor: RateLimitInterceptor = RateLimitInterceptor(),
) {
    private val gson = Gson()
    private val repositoryUrl: HttpUrl = "${baseUrl.trimEnd('/')}/repos/$owner/$repo".toHttpUrl()
    private val http = client.newBuilder()
        .connectTimeout(TIMEOUT)
        .readTimeout(TIMEOUT)
        .writeTimeout(TIMEOUT)
        .addInterceptor(GitHubApiInterceptor(token))
        .addInterceptor(rateLimitInterceptor)
        .build()

    /**
     * Newest runs first, as GitHub orders them. A filtered listing is capped by GitHub at
     * [SEARCH_RESULT_CAP] results, so the caller never receives more than that per filter.
     */
    fun listWorkflowRuns(filter: WorkflowRunFilter = WorkflowRunFilter(), limit: Int = SEARCH_RESULT_CAP): List<WorkflowRun> = paginate(limit) { page, perPage ->
        val url = repositoryUrl.newBuilder()
            .addPathSegments("actions/runs")
            .addQueryParameter("per_page", perPage.toString())
            .addQueryParameter("page", page.toString())
            .addQueryParameter("exclude_pull_requests", "true")
        filter.actor?.let { url.addQueryParameter("actor", it) }
        filter.branch?.let { url.addQueryParameter("branch", it) }
        filter.event?.let { url.addQueryParameter("event", it) }
        filter.status?.let { url.addQueryParameter("status", it.apiValue) }
        filter.createdOnOrBefore?.let { url.addQueryParameter("created", "*..$it") }
        val result = get(url.build(), WorkflowRuns::class.java)
        Page(result.totalCount, result.runs)
    }

    /** Every artifact of the run [runId], expired ones included. */
    fun listArtifacts(runId: Long): List<Artifact> = paginate(Int.MAX_VALUE) { page, perPage ->
        val url = repositoryUrl.newBuilder()
            .addPathSegments("actions/runs/$runId/artifacts")
            .addQueryParameter("per_page", perPage.toString())
            .addQueryParameter("page", page.toString())
            .build()
        val result = get(url, WorkflowArtifacts::class.java)
        Page(result.totalCount, result.artifacts)
    }

    /** Deletes the run [runId] with its logs and artifacts. @throws GitHubApiException on any non-2xx answer. */
    fun deleteWorkflowRun(runId: Long) = delete(repositoryUrl.newBuilder().addPathSegments("actions/runs/$runId").build())

    /** Deletes only the logs of the run [runId]. @throws GitHubApiException on any non-2xx answer. */
    fun deleteWorkflowRunLogs(runId: Long) = delete(repositoryUrl.newBuilder().addPathSegments("actions/runs/$runId/logs").build())

    /** Deletes the artifact [artifactId]. @throws GitHubApiException on any non-2xx answer. */
    fun deleteArtifact(artifactId: Long) = delete(repositoryUrl.newBuilder().addPathSegments("actions/artifacts/$artifactId").build())

    private data class Page<T>(val totalCount: Int, val items: List<T>)

    private fun <T> paginate(limit: Int, fetch: (page: Int, perPage: Int) -> Page<T>): List<T> {
        if (limit <= 0) return emptyList()
        val perPage = minOf(limit, MAX_PER_PAGE)
        val collected = ArrayList<T>()
        var page = 1
        while (collected.size < limit) {
            val result = fetch(page, perPage)
            collected += result.items
            if (result.items.isEmpty() || collected.size >= result.totalCount) break
            page++
        }
        return collected.take(limit)
    }

    private fun <T> get(url: HttpUrl, type: Class<T>): T = execute(Request.Builder().url(url).get().build()) { gson.fromJson(it.body.charStream(), type) }

    private fun delete(url: HttpUrl) {
        execute(Request.Builder().url(url).delete().build()) { }
    }

    private inline fun <T> execute(request: Request, onSuccess: (Response) -> T): T = http.newCall(request).execute().use { response ->
        if (!response.isSuccessful) {
            throw GitHubApiException(request.method, request.url.toString(), response.code, response.body.string())
        }
        onSuccess(response)
    }

    companion object {
        const val MAX_PER_PAGE = 100
        const val SEARCH_RESULT_CAP = 1000
        private val TIMEOUT: Duration = Duration.ofSeconds(60)
    }
}
