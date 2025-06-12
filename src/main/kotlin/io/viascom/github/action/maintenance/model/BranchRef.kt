package io.viascom.github.action.maintenance.model

import com.google.gson.annotations.SerializedName

data class BranchRef(
    @SerializedName("ref") val ref: String,
    @SerializedName("sha") val sha: String,
    @SerializedName("repo") val repo: RepoRef
)
