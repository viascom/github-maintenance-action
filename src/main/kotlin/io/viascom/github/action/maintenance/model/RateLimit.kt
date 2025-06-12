package io.viascom.github.action.maintenance.model

import com.google.gson.annotations.SerializedName

data class RateLimit(
    @SerializedName("resources")
    var resources: HashMap<String, RateLimitDetail>,

    @SerializedName("rate")
    var rate: RateLimitDetail?
)
