package io.viascom.github.action.maintenance.core

object Environment {
    val gitHubToken: String = System.getenv("INPUT_GITHUB_TOKEN")
    val githubBaseUrl: String = System.getenv("INPUT_GITHUB_BASE_URL")
    val repository: String = System.getenv("INPUT_GITHUB_REPOSITORY")
    val retentionDays = System.getenv("INPUT_RETENTION_DAYS").toLong()
    val deleteLogs = System.getenv("INPUT_DELETE_LOGS").toBoolean()
    val deleteArtifacts = System.getenv("INPUT_DELETE_ARTIFACTS").toBoolean()
    val isDryRun = System.getenv("INPUT_DRY_RUN").toBoolean()
    val isDebug = System.getenv("INPUT_DEBUG").toBoolean()
}