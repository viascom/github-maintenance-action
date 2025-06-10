package io.viascom.github.action.maintenance.api

import com.google.gson.Gson
import io.viascom.github.action.maintenance.core.Environment
import io.viascom.github.action.maintenance.model.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
class GitHubApi(
    private val gson: Gson
) {

    private val log = LoggerFactory.getLogger(javaClass)

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .addInterceptor(GitHubApiInterceptor())
        .addInterceptor(RateLimitingInterceptor())
        .build()

    fun rateLimit(): RateLimit {
        val client = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .addInterceptor(GitHubApiInterceptor())
            .build()

        val httpUrl = ("${Environment.githubBaseUrl}/rate_limit").toHttpUrl().newBuilder()

        val request = Request.Builder()
            .url(httpUrl.build())
            .get()
            .build()

        return client.newCall(request).execute().body?.string()?.let { gson.fromJson(it, RateLimit::class.java) }
            ?: throw RuntimeException("Could not load rate limits from GitHub!")
    }

    private fun listWorkflowRuns(
        owner: String,
        repo: String,
        actor: String? = null,
        branch: String? = null,
        event: String? = null,
        status: String? = null,
        perPage: Int = 100, // max. 100, default 30
        page: Int = 1,
        created: String? = null,
        keepPullRequests: Boolean = false,
        checkSuiteId: Int? = null,
        headSha: String? = null
    ): WorkflowRuns {
        val httpUrl = "${Environment.githubBaseUrl}/repos/$owner/$repo/actions/runs".toHttpUrl().newBuilder()
            .addQueryParameter("per_page", perPage.toString())
            .addQueryParameter("page", page.toString())

        actor?.let { httpUrl.addQueryParameter("actor", actor) }
        branch?.let { httpUrl.addQueryParameter("branch", branch) }
        event?.let { httpUrl.addQueryParameter("event", event) }
        status?.let { httpUrl.addQueryParameter("status", status) }
        created?.let { httpUrl.addQueryParameter("created", created) }
        keepPullRequests.let { httpUrl.addQueryParameter("keep_pull_requests", keepPullRequests.toString()) }
        checkSuiteId?.let { httpUrl.addQueryParameter("check_suite_id", checkSuiteId.toString()) }
        headSha?.let { httpUrl.addQueryParameter("head_sha", headSha) }

        val request = Request.Builder()
            .url(httpUrl.build())
            .get()
            .build()

        return client.newCall(request).execute().body?.string()?.let { gson.fromJson(it, WorkflowRuns::class.java) } ?: WorkflowRuns()
    }

    fun listWorkflowRuns(
        owner: String,
        repo: String,
        actors: List<String>? = null,
        branches: List<String>? = null,
        events: List<WorkflowRunEvent>? = null,
        statuses: List<WorkflowRunStatus>? = null,
        created: String? = null,
        keepPullRequests: Boolean = false,
        checkSuiteId: Int? = null,
        headSha: String? = null
    ): WorkflowRuns {

        var requestData = arrayListOf<RequestData>()
        actors?.let { data -> requestData = populateRequestData(data, requestData) { actor = it } }
        branches?.let { data -> requestData = populateRequestData(data, requestData) { branch = it } }
        events?.map { it.name }?.let { data -> requestData = populateRequestData(data, requestData) { event = it } }
        statuses?.map { it.name }?.let { data -> requestData = populateRequestData(data, requestData) { status = it } }

        if (requestData.isEmpty()) {
            requestData.add(RequestData())
        }

        val allWorkflowRuns = requestData.flatMap {
            listAllWorkflowRuns(
                owner,
                repo,
                it.actor,
                it.branch,
                it.event,
                it.status,
                created,
                keepPullRequests,
                checkSuiteId,
                headSha
            ).runs
        }

        return WorkflowRuns(allWorkflowRuns.size, allWorkflowRuns.toCollection(arrayListOf()))
    }

    private fun populateRequestData(
        inputData: List<String>,
        requestData: ArrayList<RequestData>,
        processor: RequestData.(String) -> Unit
    ): ArrayList<RequestData> {
        if (inputData.isEmpty()) return requestData

        return if (requestData.isEmpty()) {
            inputData.map { RequestData().apply { processor.invoke(this, it) } }
        } else {
            requestData.flatMap { data -> inputData.map { data.copy().apply { processor.invoke(this, it) } } }
        }.toCollection(arrayListOf())
    }

    private fun listAllWorkflowRuns(
        owner: String,
        repo: String,
        actor: String?,
        branch: String?,
        event: String?,
        status: String?,
        created: String?,
        keepPullRequests: Boolean,
        checkSuiteId: Int?,
        headSha: String?,
    ): WorkflowRuns {
        val workflowRuns =
            listWorkflowRuns(owner, repo, actor, branch, event, status, 100, 1, created, keepPullRequests, checkSuiteId, headSha)
        val remaining = workflowRuns.totalCount - 100
        if (remaining >= 0) {
            val totalPages = remaining / 100 + 1
            for (i in 1..totalPages) {
                workflowRuns.runs.addAll(
                    listWorkflowRuns(
                        owner = owner,
                        repo = repo,
                        actor = actor,
                        branch = branch,
                        event = event,
                        status = status,
                        perPage = 100,
                        page = i + 1,
                        created = created,
                        keepPullRequests = keepPullRequests,
                        checkSuiteId = checkSuiteId,
                        headSha = headSha
                    ).runs
                )
            }
        }

        log.info("\uD83D\uDD0D Found ${workflowRuns.runs.size} workflow runs.")

        return workflowRuns
    }

    fun deleteWorkflowRun(owner: String, repo: String, runId: Long): Response {
        val request = Request.Builder()
            .url("${Environment.githubBaseUrl}/repos/$owner/$repo/actions/runs/$runId")
            .delete()
            .build()

        val response: Response
        if (!Environment.isDryRun) {
            response = client.newCall(request).execute()
            log.info("\uD83E\uDEE7 Deleted workflow run with id $runId.")
        } else {
            log.info("\uD83E\uDD21 Pretending to deleted workflow run with id $runId.")
            response = createMockResponse()
        }

        return response
    }

    fun deleteWorkflowRunLogs(owner: String, repo: String, runId: Long): Response {
        val request = Request.Builder()
            .url("${Environment.githubBaseUrl}/repos/$owner/$repo/actions/runs/$runId/logs")
            .delete()
            .build()

        val response: Response
        if (!Environment.isDryRun) {
            response = client.newCall(request).execute()
            log.info("\uD83D\uDCC4 Deleted logs of workflow run $runId.")
        } else {
            log.info("\uD83E\uDD21 Pretending to deleted logs of workflow run $runId.")
            response = createMockResponse()
        }

        return response
    }

    private fun listWorkflowRunArtifacts(
        owner: String,
        repo: String,
        runId: Long,
        perPage: Int = 30, // max. 100
        page: Int = 1,
        name: String? = null
    ): WorkflowArtifacts {
        val httpUrl = "${Environment.githubBaseUrl}/repos/$owner/$repo/actions/runs/$runId/artifacts".toHttpUrl().newBuilder()
            .addQueryParameter("per_page", perPage.toString())
            .addQueryParameter("page", page.toString())

        name?.let { httpUrl.addQueryParameter("name", name) }

        val request = Request.Builder()
            .url("${Environment.githubBaseUrl}/repos/$owner/$repo/actions/runs/$runId/logs")
            .get()
            .build()

        return client.newCall(request).execute().body?.string()?.let { gson.fromJson(it, WorkflowArtifacts::class.java) } ?: WorkflowArtifacts()
    }

    fun listWorkflowRunArtifacts(
        owner: String,
        repo: String,
        runId: Long,
    ): WorkflowArtifacts {
        val workflowRunArtifacts = listWorkflowRunArtifacts(owner, repo, runId, 100, 1)
        val remaining = workflowRunArtifacts.totalCount - 100
        if (remaining >= 0) {
            val totalPages = remaining / 100 + 1
            for (i in 1..totalPages) {
                workflowRunArtifacts.artifacts.addAll(
                    listWorkflowRunArtifacts(
                        owner = owner,
                        repo = repo,
                        runId = runId,
                        perPage = 100,
                        page = i + 1
                    ).artifacts
                )
            }
        }

        log.info("\uD83D\uDD0D Found ${workflowRunArtifacts.artifacts.size} artifacts in workflow run $runId.")

        return workflowRunArtifacts
    }

    fun deleteArtifact(owner: String, repo: String, artifactId: Long): Response {
        val request = Request.Builder()
            .url("${Environment.githubBaseUrl}/repos/$owner/$repo/actions/artifacts/$artifactId")
            .delete()
            .build()

        val response: Response
        if (!Environment.isDryRun) {
            response = client.newCall(request).execute()
            log.info("\uD83D\uDCC4 Deleted workflow artifact with id $artifactId.")
        } else {
            log.info("\uD83E\uDD21 Pretending to deleted workflow artifact with id $artifactId.")
            response = createMockResponse()
        }

        return response
    }

    private fun createMockResponse(): Response {
        // Mock a successful response with a status code of 200 (OK)
        return Response.Builder()
            .code(204) // 204 No Content
            .protocol(Protocol.HTTP_1_1)
            .message("GitHub API call simulated.")
            .request(Request.Builder().url("http://mockurl/").build())
            .build()
    }
}
