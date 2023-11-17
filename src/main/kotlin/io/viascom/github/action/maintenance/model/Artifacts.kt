package io.viascom.github.action.maintenance.model

import com.google.gson.annotations.SerializedName


data class Artifacts(

    @SerializedName("id")
    var id: Long = 0,

    @SerializedName("node_id")
    var nodeId: String? = null,

    @SerializedName("name")
    var name: String? = null,

    @SerializedName("size_in_bytes")
    var sizeInBytes: Int? = null,

    @SerializedName("url")
    var url: String? = null,

    @SerializedName("archive_download_url")
    var archiveDownloadUrl: String? = null,

    @SerializedName("expired")
    var expired: Boolean? = null,

    @SerializedName("created_at")
    var createdAt: String? = null,

    @SerializedName("expires_at")
    var expiresAt: String? = null,

    @SerializedName("updated_at")
    var updatedAt: String? = null,

    @SerializedName("workflow_run")
    var workflowRun: WorkflowRun? = WorkflowRun()

)