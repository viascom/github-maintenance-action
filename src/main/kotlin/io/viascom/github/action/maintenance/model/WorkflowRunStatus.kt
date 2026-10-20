package io.viascom.github.action.maintenance.model

/**
 * The values GitHub accepts for the `status` query parameter of the workflow-runs
 * listing. The REST API spells them in lower case.
 */
enum class WorkflowRunStatus {
    COMPLETED,
    ACTION_REQUIRED,
    CANCELLED,
    FAILURE,
    NEUTRAL,
    SKIPPED,
    STALE,
    SUCCESS,
    TIMED_OUT,
    IN_PROGRESS,
    QUEUED,
    REQUESTED,
    WAITING,
    PENDING,
    ;

    val apiValue: String = name.lowercase()

    companion object {
        fun parse(value: String): WorkflowRunStatus = entries.find { it.apiValue == value.trim().lowercase() }
            ?: throw IllegalArgumentException(
                "unknown workflow run status '$value'; allowed: ${entries.joinToString { it.apiValue }}",
            )
    }
}
