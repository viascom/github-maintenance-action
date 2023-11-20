package io.viascom.github.action.maintenance.core

import io.viascom.github.action.maintenance.model.WorkflowRunEvent
import io.viascom.github.action.maintenance.model.WorkflowRunStatus

object Environment {
    val gitHubToken: String = System.getenv("INPUT_GITHUB_TOKEN")
    val githubBaseUrl: String = System.getenv("INPUT_GITHUB_BASE_URL")
    val repository: String = System.getenv("INPUT_REPOSITORY")
    val retentionDays = System.getenv("INPUT_RETENTION_DAYS").toLong()
    val deleteLogs = System.getenv("INPUT_DELETE_LOGS").toBoolean()
    val deleteArtifacts = System.getenv("INPUT_DELETE_ARTIFACTS").toBoolean()
    val actors: List<String> = System.getenv("INPUT_ACTORS").split(",").map(String::trim).filter { it.isNotBlank() }
    val branches: List<String> = System.getenv("INPUT_BRANCHES").split(",").map(String::trim).filter { it.isNotBlank() }
    val events = WorkflowRunEvent.fromCommaSeparatedValues(System.getenv("INPUT_EVENTS"))
    val statuses = WorkflowRunStatus.fromCommaSeparatedValues(System.getenv("INPUT_STATUSES"))
    val isExcludePullRequests = System.getenv("INPUT_EXCLUDE_PULL_REQUESTS").toBoolean()
    val isDryRun = System.getenv("INPUT_DRY_RUN").toBoolean()
    val isDebug = System.getenv("INPUT_DEBUG").toBoolean()
}