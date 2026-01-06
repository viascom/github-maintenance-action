package io.viascom.github.action.maintenance.core

import io.github.bucket4j.Bucket
import io.viascom.github.action.maintenance.model.WorkflowRunEvent
import io.viascom.github.action.maintenance.model.WorkflowRunStatus
import io.viascom.github.action.maintenance.util.fromCommaSeparatedValues
import io.viascom.github.action.maintenance.util.splitCommaList

object Environment {

    // REQUIRED
    val gitHubToken: String = System.getenv("INPUT_GITHUB_TOKEN")?.takeIf { it.isNotBlank() } ?: error("INPUT_GITHUB_TOKEN is required")
    val repository: String = System.getenv("INPUT_REPOSITORY")?.takeIf { it.isNotBlank() } ?: error("INPUT_REPOSITORY is required")
    val retentionDays: Int = System.getenv("INPUT_RETENTION_DAYS")?.takeIf { it.isNotBlank() }?.toIntOrNull()
        ?: error("INPUT_RETENTION_DAYS must be a valid integer")
    val keepMinimumRuns: Int = System.getenv("INPUT_KEEP_MINIMUM_RUNS")?.takeIf { it.isNotBlank() }?.toIntOrNull()
        ?: error("INPUT_KEEP_MINIMUM_RUNS must be a valid integer")

    // OPTIONAL
    val githubBaseUrl: String = System.getenv("INPUT_GITHUB_BASE_URL")?.takeIf { it.isNotBlank() } ?: "https://api.github.com"
    val deleteLogs: Boolean = System.getenv("INPUT_DELETE_LOGS")?.toBoolean() ?: false
    val deleteArtifacts: Boolean = System.getenv("INPUT_DELETE_ARTIFACTS")?.toBoolean() ?: false
    val actors: List<String> = System.getenv("INPUT_ACTORS").splitCommaList()
    val branches: List<String> = System.getenv("INPUT_BRANCHES").splitCommaList()
    val events: List<WorkflowRunEvent> = System.getenv("INPUT_EVENTS").fromCommaSeparatedValues()
    val statuses: List<WorkflowRunStatus> = System.getenv("INPUT_STATUSES").fromCommaSeparatedValues()
    val isKeepPullRequests: Boolean = System.getenv("INPUT_KEEP_PULL_REQUESTS")?.toBoolean() ?: false
    val isDryRun: Boolean = System.getenv("INPUT_DRY_RUN")?.toBoolean() ?: false
    val isDebug: Boolean = System.getenv("INPUT_DEBUG")?.toBoolean() ?: false

    @Volatile
    lateinit var rateLimiter: Bucket

    fun validate() {
        require(gitHubToken.isNotBlank()) { "INPUT_GITHUB_TOKEN is required" }
        require(repository.contains("/")) { "INPUT_REPOSITORY must be in format 'owner/repo'" }
        require(retentionDays >= 0) { "INPUT_RETENTION_DAYS must be >= 0" }
        require(keepMinimumRuns >= 0) { "INPUT_KEEP_MINIMUM_RUNS must be >= 0" }
    }
}
