package io.viascom.github.action.maintenance.model

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
}
