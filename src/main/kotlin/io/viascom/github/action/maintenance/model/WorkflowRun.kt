package io.viascom.github.action.maintenance.model

import com.google.gson.annotations.SerializedName

data class WorkflowRuns(@SerializedName("total_count") val totalCount: Int = 0, @SerializedName("workflow_runs") val runs: List<WorkflowRun> = emptyList())

data class WorkflowRun(
    val id: Long,
    val name: String? = null,
    val event: String? = null,
    val status: String? = null,
    val conclusion: String? = null,
    @SerializedName("head_branch") val headBranch: String? = null,
    @SerializedName("created_at") val createdAt: String? = null,
) {
    val isPullRequestRun: Boolean
        get() = event == "pull_request" || event == "pull_request_target"
}
