package io.viascom.github.action.maintenance.core

import io.viascom.github.action.maintenance.api.GitHubApi
import io.viascom.github.action.maintenance.model.WorkflowRunStatus
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
class MaintenanceExecutor(
    private val githubApi: GitHubApi
) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun deleteOldActionRuns(owner: String, repo: String, retentionDays: Long) {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val formattedDate = LocalDateTime.now().minusDays(retentionDays).format(formatter)

        val workflowRuns = githubApi.listWorkflowRuns(
            owner = owner,
            repo = repo,
            actors = Environment.actors,
            branches = Environment.branches,
            events = Environment.events,
            statuses = Environment.statuses,
            created = "*..$formattedDate",
            excludePullRequests = Environment.isExcludePullRequests,
        )

        workflowRuns.runs.forEach { run ->
            if (Environment.deleteLogs) {
                githubApi.deleteWorkflowRunLogs(owner, repo, run.id)
            }

            if (Environment.deleteArtifacts) {
                val workflowRunArtifacts = githubApi.listWorkflowRunArtifacts(owner, repo, run.id)
                workflowRunArtifacts.artifacts.forEach { artifact ->
                    githubApi.deleteArtifact(owner, repo, artifact.id)
                }
            }

            if (!Environment.deleteLogs && !Environment.deleteArtifacts) {
                githubApi.deleteWorkflowRun(owner, repo, run.id)
            }
        }
    }

    private fun List<WorkflowRunStatus>.toCommaSeparatedString(): String {
        return this.joinToString(separator = ", ") { it.value }
    }
}
