package io.viascom.github.action.maintenance.api

import io.viascom.github.action.maintenance.model.WorkflowRunStatus
import java.time.LocalDate

/** One combination of the single-valued filters the workflow-runs listing accepts. */
data class WorkflowRunFilter(
    val actor: String? = null,
    val branch: String? = null,
    val event: String? = null,
    val status: WorkflowRunStatus? = null,
    val createdOnOrBefore: LocalDate? = null,
)
