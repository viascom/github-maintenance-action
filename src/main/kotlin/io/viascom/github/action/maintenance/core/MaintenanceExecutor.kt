package io.viascom.github.action.maintenance.core

import io.viascom.github.action.maintenance.api.GitHubApi
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

@Service
class MaintenanceExecutor(private val githubApi: GitHubApi) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun deleteOldActionRuns(owner: String, repo: String, retentionDays: Int) {
        log.info("Starting maintenance for $owner/$repo (retentionDays=$retentionDays)")

        val keepMinimumRuns = Environment.keepMinimumRuns
        val actors = Environment.actors
        val branches = Environment.branches
        val events = Environment.events
        val statuses = Environment.statuses
        val deleteLogs = Environment.deleteLogs
        val deleteArtifacts = Environment.deleteArtifacts
        val keepPullRequests = Environment.isKeepPullRequests

        val allWorkflowRuns = githubApi.listWorkflowRuns(owner = owner, repo = repo)

        if (allWorkflowRuns.totalCount <= keepMinimumRuns) {
            log.info("Total workflow runs (${allWorkflowRuns.totalCount}) <= keepMinimumRuns ($keepMinimumRuns). Skipping maintenance.")
            return
        }

        val runsToKeep = allWorkflowRuns.runs.take(keepMinimumRuns).map { it.id }.toSet()
        log.info("Keeping $keepMinimumRuns latest workflow runs: $runsToKeep")

        val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val formattedDate = LocalDateTime.now().minusDays(retentionDays.toLong()).format(dateFormatter)

        val workflowRuns = githubApi.listWorkflowRuns(
            owner = owner,
            repo = repo,
            actors = actors,
            branches = branches,
            events = events,
            statuses = statuses,
            created = "*..$formattedDate",
            keepPullRequests = keepPullRequests,
        )

        val runsToProcess = workflowRuns.runs.filter { it.id !in runsToKeep }

        log.info("Found ${runsToProcess.size} workflow runs to process (older than $formattedDate)")

        var runsDeleted = 0
        var artifactsDeleted = 0
        var logsDeleted = 0

        runsToProcess.forEach { run ->
            log.info("Processing workflow run [id=${run.id}, name=${run.name}, status=${run.status}]")

            if (deleteLogs) {
                runCatching {
                    log.info("Deleting logs for workflow run ${run.id}")
                    githubApi.deleteWorkflowRunLogs(owner, repo, run.id)
                    logsDeleted++
                }.onFailure { log.warn("Failed to delete logs for run ${run.id}: ${it.message}") }

                applyApiDelay()
            }

            if (deleteArtifacts) {
                runCatching {
                    val workflowRunArtifacts = githubApi.listWorkflowRunArtifacts(owner, repo, run.id)
                    workflowRunArtifacts.artifacts.forEach { artifact ->
                        log.info("Deleting artifact [id=${artifact.id}, name=${artifact.name}]")
                        githubApi.deleteArtifact(owner, repo, artifact.id)
                        artifactsDeleted++
                        applyApiDelay()
                    }
                }.onFailure { log.warn("Failed to delete artifacts for run ${run.id}: ${it.message}") }
            }

            if (!deleteLogs && !deleteArtifacts) {
                runCatching {
                    log.info("Deleting entire workflow run ${run.id}")
                    githubApi.deleteWorkflowRun(owner, repo, run.id)
                    runsDeleted++
                }.onFailure { log.warn("Failed to delete workflow run ${run.id}: ${it.message}") }

                applyApiDelay()
            }
        }

        log.info("Maintenance completed for $owner/$repo")
        log.info("Summary: runsDeleted=$runsDeleted, artifactsDeleted=$artifactsDeleted, logsDeleted=$logsDeleted")
    }

    private fun applyApiDelay() {
        try {
            TimeUnit.MILLISECONDS.sleep(1000L)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        }
    }
}
