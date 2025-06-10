package io.viascom.github.action.maintenance.model

import com.google.gson.annotations.SerializedName

data class PullRequestRef(
    @SerializedName("id") val id: Long,
    @SerializedName("number") val number: Int,
    @SerializedName("url") val url: String,
    @SerializedName("head") val head: BranchRef,
    @SerializedName("base") val base: BranchRef
)
