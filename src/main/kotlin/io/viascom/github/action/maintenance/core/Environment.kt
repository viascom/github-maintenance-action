package io.viascom.github.action.maintenance.core

import io.viascom.github.action.maintenance.model.WorkflowRunEvent
import io.viascom.github.action.maintenance.model.WorkflowRunStatus
import io.viascom.github.action.maintenance.util.fromCommaSeparatedValues

object Environment {
    val gitHubToken: String = System.getenv("INPUT_GITHUB_TOKEN")
    val githubBaseUrl: String = System.getenv("INPUT_GITHUB_BASE_URL")
    val repository: String = System.getenv("INPUT_REPOSITORY")
    val retentionDays = System.getenv("INPUT_RETENTION_DAYS").toInt()
    val keepMinimumRuns = System.getenv("INPUT_KEEP_MINIMUM_RUNS").toInt()
    val deleteLogs = System.getenv("INPUT_DELETE_LOGS").toBoolean()
    val deleteArtifacts = System.getenv("INPUT_DELETE_ARTIFACTS").toBoolean()
    val actors: List<String> = System.getenv("INPUT_ACTORS").split(",").map(String::trim).filter { it.isNotBlank() }
    val branches: List<String> = System.getenv("INPUT_BRANCHES").split(",").map(String::trim).filter { it.isNotBlank() }
    val events: List<WorkflowRunEvent> = System.getenv("INPUT_EVENTS").fromCommaSeparatedValues()
    val statuses: List<WorkflowRunStatus> = System.getenv("INPUT_STATUSES").fromCommaSeparatedValues()
    val isKeepPullRequests = System.getenv("INPUT_KEEP_PULL_REQUESTS").toBoolean()
    val isDryRun = System.getenv("INPUT_DRY_RUN").toBoolean()
    val isDebug = System.getenv("INPUT_DEBUG").toBoolean()
}