package io.viascom.github.action.maintenance.core

import io.viascom.github.action.maintenance.api.GitHubApi
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
class MaintenanceExecutor(
    private val githubApi: GitHubApi
) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun deleteOldActionRuns(owner: String, repo: String, retentionDays: Int) {

        val allWorkflowRuns = githubApi.listWorkflowRuns(
            owner = owner,
            repo = repo,
        )

        if (allWorkflowRuns.totalCount <= Environment.keepMinimumRuns) return
        val runsToKeep = allWorkflowRuns.runs.take(Environment.keepMinimumRuns).map { it.id }.toSet()

        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val formattedDate = LocalDateTime.now().minusDays(retentionDays.toLong()).format(formatter)

        val workflowRuns = githubApi.listWorkflowRuns(
            owner = owner,
            repo = repo,
            actors = Environment.actors,
            branches = Environment.branches,
            events = Environment.events,
            statuses = Environment.statuses,
            created = "*..$formattedDate",
            keepPullRequests = Environment.isKeepPullRequests,
        )

        val runsToProcess = workflowRuns.runs.filter { it.id !in runsToKeep }

        runsToProcess.forEach { run ->
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
                Thread.sleep(Duration.ofSeconds(1))
            }
        }
    }
}
