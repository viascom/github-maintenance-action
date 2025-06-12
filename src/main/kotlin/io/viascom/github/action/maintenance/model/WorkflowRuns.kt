package io.viascom.github.action.maintenance.model

import com.google.gson.annotations.SerializedName

data class WorkflowRuns(
    @SerializedName("total_count")
    var totalCount: Int = 0,

    @SerializedName("workflow_runs")
    var runs: ArrayList<WorkflowRun> = arrayListOf()
)
