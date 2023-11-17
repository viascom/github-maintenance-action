package io.viascom.github.action.maintenance.core

import io.viascom.github.action.maintenance.api.GitHubApi
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class MaintenanceExecutor {

    private val log = LoggerFactory.getLogger(javaClass)

    fun deleteOldActionRuns(owner: String, repo: String, retentionDays: Long) {
        val githubApi = GitHubApi(Environment.githubBaseUrl)

        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val formattedDate = LocalDateTime.now().minusDays(retentionDays).format(formatter)

        val workflowRuns = githubApi.listWorkflowRuns(
            owner = owner,
            repo = repo,
            created = "*..$formattedDate",
        )

        log.info("\uD83D\uDD0D Found ${workflowRuns.runs.size} workflow runs to clean up ...")
        workflowRuns.runs.forEach { run ->
            if (Environment.deleteLogs) {
                githubApi.deleteWorkflowRunLogs(owner, repo, run.id)
            }

            if (Environment.deleteArtifacts) {
                val workflowRunArtifacts = githubApi.listWorkflowRunArtifacts(owner, repo, run.id)
                if (workflowRunArtifacts.totalCount == 0) {
                    log.info("\uD83D\uDD0D No artifacts found to clean up for workflow run with id ${run.id}.")
                } else {
                    workflowRunArtifacts.artifacts.forEach { artifact ->
                        githubApi.deleteArtifact(owner, repo, artifact.id)
                    }
                }
            }

            if (!Environment.deleteLogs && !Environment.deleteArtifacts) {
                githubApi.deleteWorkflowRun(owner, repo, run.id)
            }
        }
    }
}
