package io.viascom.github.action.maintenance.model

import com.google.gson.annotations.SerializedName

data class WorkflowArtifacts(@SerializedName("total_count") val totalCount: Int = 0, val artifacts: List<Artifact> = emptyList())

data class Artifact(val id: Long, val name: String? = null, @SerializedName("size_in_bytes") val sizeInBytes: Long? = null, val expired: Boolean? = null)
