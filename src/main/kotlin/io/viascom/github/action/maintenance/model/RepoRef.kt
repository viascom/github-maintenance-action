package io.viascom.github.action.maintenance.model

import com.google.gson.annotations.SerializedName

data class RepoRef(
    @SerializedName("id") val id: Long,
    @SerializedName("url") val url: String,
    @SerializedName("name") val name: String
)
