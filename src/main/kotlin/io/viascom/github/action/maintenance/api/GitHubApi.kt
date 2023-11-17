package io.viascom.github.action.maintenance.api

import com.google.gson.Gson
import io.viascom.github.action.maintenance.core.Environment
import io.viascom.github.action.maintenance.model.WorkflowArtifacts
import io.viascom.github.action.maintenance.model.WorkflowRuns
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit


class GitHubApi(private val baseUrl: String) {

    private val log = LoggerFactory.getLogger(javaClass)

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .addInterceptor(GitHubApiInterceptor())
        .build()

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
        excludePullRequests: Boolean = false,
        checkSuiteId: Int? = null,
        headSha: String? = null
    ): WorkflowRuns {
        val httpUrl = "$baseUrl/repos/$owner/$repo/actions/runs".toHttpUrl().newBuilder()
            .addQueryParameter("per_page", perPage.toString())
            .addQueryParameter("page", page.toString())

        actor?.let { httpUrl.addQueryParameter("actor", actor) }
        branch?.let { httpUrl.addQueryParameter("branch", branch) }
        event?.let { httpUrl.addQueryParameter("event", event) }
        status?.let { httpUrl.addQueryParameter("status", status) }
        created?.let { httpUrl.addQueryParameter("created", created) }
        excludePullRequests.let { httpUrl.addQueryParameter("exclude_pull_requests", excludePullRequests.toString()) }
        checkSuiteId?.let { httpUrl.addQueryParameter("check_suite_id", checkSuiteId.toString()) }
        headSha?.let { httpUrl.addQueryParameter("head_sha", headSha) }

        val request = Request.Builder()
            .url(httpUrl.build())
            .get()
            .build()

        return client.newCall(request).execute().body?.string()?.let { Gson().fromJson(it, WorkflowRuns::class.java) } ?: WorkflowRuns()
    }

    fun listWorkflowRuns(
        owner: String,
        repo: String,
        actor: String? = null,
        branch: String? = null,
        event: String? = null,
        status: String? = null,
        created: String? = null,
        excludePullRequests: Boolean = false,
        checkSuiteId: Int? = null,
        headSha: String? = null
    ): WorkflowRuns {
        val workflowRuns = listWorkflowRuns(owner, repo, actor, branch, event, status, 100, 1, created, excludePullRequests, checkSuiteId, headSha)
        val remaining = workflowRuns.totalCount - 100
        if (remaining <= 0) {
            return workflowRuns
        }

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
                    excludePullRequests = excludePullRequests,
                    checkSuiteId = checkSuiteId,
                    headSha = headSha
                ).runs
            )
        }
        return workflowRuns
    }

    fun deleteWorkflowRun(owner: String, repo: String, runId: Long): Response {
        val request = Request.Builder()
            .url("$baseUrl/repos/$owner/$repo/actions/runs/$runId")
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
            .url("$baseUrl/repos/$owner/$repo/actions/runs/$runId/logs")
            .delete()
            .build()

        val response: Response
        if (!Environment.isDryRun) {
            response = client.newCall(request).execute()
            log.info("\uD83D\uDCC4 Deleted workflow logs for run with id $runId.")
        } else {
            log.info("\uD83E\uDD21 Pretending to deleted workflow logs for run with id $runId.")
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
        val httpUrl = "$baseUrl/repos/$owner/$repo/actions/runs/$runId/artifacts".toHttpUrl().newBuilder()
            .addQueryParameter("per_page", perPage.toString())
            .addQueryParameter("page", page.toString())

        name?.let { httpUrl.addQueryParameter("name", name) }

        val request = Request.Builder()
            .url("$baseUrl/repos/$owner/$repo/actions/runs/$runId/logs")
            .get()
            .build()

        return client.newCall(request).execute().body?.string()?.let { Gson().fromJson(it, WorkflowArtifacts::class.java) } ?: WorkflowArtifacts()
    }

    fun listWorkflowRunArtifacts(
        owner: String,
        repo: String,
        runId: Long,
    ): WorkflowArtifacts {
        val workflowRunArtifacts = listWorkflowRunArtifacts(owner, repo, runId, 100, 1)
        val remaining = workflowRunArtifacts.totalCount - 100
        if (remaining <= 0) {
            return workflowRunArtifacts
        }

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
        return workflowRunArtifacts
    }

    fun deleteArtifact(owner: String, repo: String, artifactId: Long): Response {
        val request = Request.Builder()
            .url("$baseUrl/repos/$owner/$repo/actions/artifacts/$artifactId")
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