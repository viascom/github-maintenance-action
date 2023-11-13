package io.viascom.github.action.maintenance.api

import com.google.gson.Gson
import io.viascom.github.action.maintenance.model.WorkflowArtifacts
import io.viascom.github.action.maintenance.model.WorkflowRuns
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response


class GitHubApi(private val baseUrl: String) {

    fun listWorkflowRuns(
        owner: String,
        repo: String,
        actor: String? = null,
        branch: String? = null,
        event: String? = null,
        status: String? = null,
        perPage: Int = 30, // max. 100
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
            .header("Accept", "application/vnd.github+json")
            .header("Authorization", "Bearer ${System.getenv("GITHUB_TOKEN")}")
            .get()
            .build()

        return OkHttpClient().newCall(request).execute().body?.string()?.let { Gson().fromJson(it, WorkflowRuns::class.java) } ?: WorkflowRuns()
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
            .header("accept", "application/vnd.github+json")
            .delete()
            .build()

        return OkHttpClient().newCall(request).execute()
    }

    fun deleteWorkflowRunLogs(owner: String, repo: String, runId: Long): Response {
        val request = Request.Builder()
            .url("$baseUrl/repos/$owner/$repo/actions/runs/$runId/logs")
            .header("accept", "application/vnd.github+json")
            .delete()
            .build()

        return OkHttpClient().newCall(request).execute()
    }

    fun listWorkflowRunArtifacts(
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
            .header("accept", "application/vnd.github+json")
            .get()
            .build()

        return OkHttpClient().newCall(request).execute().body?.string()?.let { Gson().fromJson(it, WorkflowArtifacts::class.java) } ?: WorkflowArtifacts()
    }
}