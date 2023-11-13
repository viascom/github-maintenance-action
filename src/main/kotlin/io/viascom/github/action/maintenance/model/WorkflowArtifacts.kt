package io.viascom.github.action.maintenance.model

import com.google.gson.annotations.SerializedName


data class WorkflowArtifacts(

    @SerializedName("total_count")
    var totalCount: Int? = null,

    @SerializedName("artifacts")
    var artifacts: ArrayList<Artifacts> = arrayListOf()

)