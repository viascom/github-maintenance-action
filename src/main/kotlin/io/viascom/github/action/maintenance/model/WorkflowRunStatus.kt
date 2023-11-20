package io.viascom.github.action.maintenance.model

enum class WorkflowRunStatus(val value: String) {
    COMPLETED("completed"),
    ACTION_REQUIRED("action_required"),
    CANCELLED("cancelled"),
    FAILURE("failure"),
    NEUTRAL("neutral"),
    SKIPPED("skipped"),
    STALE("stale"),
    SUCCESS("success"),
    TIMED_OUT("timed_out"),
    IN_PROGRESS("in_progress"),
    QUEUED("queued"),
    REQUESTED("requested"),
    WAITING("waiting"),
    PENDING("pending");

    companion object {
        private val map = entries.associateBy(WorkflowRunStatus::value)
        fun fromValue(value: String) = map[value]

        fun fromCommaSeparatedValues(values: String): List<WorkflowRunStatus> {
            if (values.isBlank()) {
                return arrayListOf()
            }

            return values.split(",")
                .map { it.trim() }
                .mapNotNull { fromValue(it) }
        }

        fun toCommaSeparatedString(statuses: List<WorkflowRunStatus>): String {
            return statuses.joinToString(separator = ", ") { it.value }
        }
    }
}
