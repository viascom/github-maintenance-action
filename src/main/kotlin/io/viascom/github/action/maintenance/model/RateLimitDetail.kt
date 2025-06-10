package io.viascom.github.action.maintenance.model

import com.google.gson.annotations.SerializedName

data class RateLimitDetail(
    @SerializedName("limit")
    var limit: Long,

    @SerializedName("used")
    var used: Long,

    @SerializedName("remaining")
    var remaining: Long,

    @SerializedName("reset")
    var reset: Long
)
