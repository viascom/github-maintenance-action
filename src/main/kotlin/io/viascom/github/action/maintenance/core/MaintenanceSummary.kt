package io.viascom.github.action.maintenance.core

/** The counts of one maintenance pass. In a dry run the deleted counts are what would have been deleted. */
data class MaintenanceSummary(val dryRun: Boolean, val candidates: Int, val runsDeleted: Int, val logsDeleted: Int, val artifactsDeleted: Int, val failures: Int) {
    val verb: String
        get() = if (dryRun) "would delete" else "deleted"

    /** @throws IllegalStateException when at least one operation failed, so the action step fails. */
    fun requireNoFailures() {
        check(failures == 0) { "$failures of the maintenance operations failed; see the log above" }
    }
}
