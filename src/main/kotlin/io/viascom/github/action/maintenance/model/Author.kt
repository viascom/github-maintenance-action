package io.viascom.github.action.maintenance.model

import com.google.gson.annotations.SerializedName

data class Author(
    @SerializedName("name")
    var name: String? = null,

    @SerializedName("email")
    var email: String? = null
)
