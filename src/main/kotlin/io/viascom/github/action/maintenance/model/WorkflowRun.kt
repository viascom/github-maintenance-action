package io.viascom.github.action.maintenance.model

import com.google.gson.annotations.SerializedName


data class WorkflowRun(

    @SerializedName("id")
    var id: Long,

    @SerializedName("name")
    var name: String? = null,

    @SerializedName("node_id")
    var nodeId: String? = null,

    @SerializedName("check_suite_id")
    var checkSuiteId: Long? = null,

    @SerializedName("check_suite_node_id")
    var checkSuiteNodeId: String? = null,

    @SerializedName("head_branch")
    var headBranch: String? = null,

    @SerializedName("head_sha")
    var headSha: String? = null,

    @SerializedName("path")
    var path: String? = null,

    @SerializedName("run_number")
    var runNumber: Long? = null,

    @SerializedName("event")
    var event: String? = null,

    @SerializedName("display_title")
    var displayTitle: String? = null,

    @SerializedName("status")
    var status: String? = null,

    @SerializedName("conclusion")
    var conclusion: String? = null,

    @SerializedName("workflow_id")
    var workflowId: Long? = null,

    @SerializedName("url")
    var url: String? = null,

    @SerializedName("html_url")
    var htmlUrl: String? = null,

    @SerializedName("pull_requests")
    var pullRequests: ArrayList<String> = arrayListOf(),

    @SerializedName("created_at")
    var createdAt: String? = null,

    @SerializedName("updated_at")
    var updatedAt: String? = null,

    @SerializedName("actor")
    var actor: Actor? = Actor(),

    @SerializedName("run_attempt")
    var runAttempt: Long? = null,

    @SerializedName("run_started_at")
    var runStartedAt: String? = null,

    @SerializedName("triggering_actor")
    var triggeringActor: Actor? = Actor(),

    @SerializedName("jobs_url")
    var jobsUrl: String? = null,

    @SerializedName("logs_url")
    var logsUrl: String? = null,

    @SerializedName("check_suite_url")
    var checkSuiteUrl: String? = null,

    @SerializedName("artifacts_url")
    var artifactsUrl: String? = null,

    @SerializedName("cancel_url")
    var cancelUrl: String? = null,

    @SerializedName("rerun_url")
    var rerunUrl: String? = null,

    @SerializedName("workflow_url")
    var workflowUrl: String? = null,

    @SerializedName("head_commit")
    var headCommit: HeadCommit? = HeadCommit(),

    @SerializedName("repository")
    var repository: Repository? = Repository(),

    @SerializedName("head_repository")
    var headRepository: HeadRepository? = HeadRepository()

)