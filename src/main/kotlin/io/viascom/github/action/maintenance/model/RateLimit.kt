package io.viascom.github.action.maintenance.model

import com.google.gson.annotations.SerializedName

data class RateLimit(
    @SerializedName("resources")
    val resources: Map<String, RateLimitDetail>,

    @SerializedName("rate")
    val rate: RateLimitDetail?
)
