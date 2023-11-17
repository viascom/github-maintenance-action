package io.viascom.github.action.maintenance.core

object Environment {
    val gitHubToken = System.getenv("github_token").toString()
    val githubBaseUrl = System.getenv("github_base_url") ?: "https://api.github.com"
    val repository = System.getenv("github_repository") ?: ""
    val retentionDays = System.getenv("retention_days").toLong()
    val deleteLogs = System.getenv("delete_logs").toBoolean()
    val deleteArtifacts = System.getenv("delete_artifacts").toBoolean()
    val isDryRun = System.getenv("dry_run").toBoolean()
    val isDebug = System.getenv("debug").toBoolean()
}