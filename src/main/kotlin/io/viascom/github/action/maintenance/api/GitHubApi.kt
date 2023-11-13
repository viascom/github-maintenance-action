package io.viascom.github.action.maintenance.api

import com.google.gson.Gson
import io.viascom.github.action.maintenance.model.WorkflowRuns
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response


class GitHubApi {

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
        val httpUrl = "https://api.github.com/repos/$owner/$repo/actions/runs".toHttpUrl().newBuilder()
            .addQueryParameter("per_page", perPage.toString())
            .addQueryParameter("page", page.toString())

        created?.let { httpUrl.addQueryParameter("created", created) }

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
            .url("https://api.github.com/repos/$owner/$repo/actions/runs/$runId")
            .header("accept", "application/vnd.github+json")
            .delete()
            .build()
        return OkHttpClient().newCall(request).execute()
    }
}