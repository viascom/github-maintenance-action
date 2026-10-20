package io.viascom.github.action.maintenance.core

import io.viascom.github.action.maintenance.api.GitHubApi
import io.viascom.github.action.maintenance.model.WorkflowRun
import org.slf4j.LoggerFactory
import java.time.Clock
import java.time.Duration
import java.time.LocalDate

/**
 * Runs one maintenance pass over the repository named in [settings]: lists the candidates,
 * deletes what the settings ask for, and reports the counts.
 */
class MaintenanceExecutor(
    private val api: GitHubApi,
    private val settings: Settings,
    private val clock: Clock = Clock.systemUTC(),
    private val pause: (Duration) -> Unit = { Thread.sleep(it) },
) {
    private val log = LoggerFactory.getLogger(javaClass)

    private var runsDeleted = 0
    private var logsDeleted = 0
    private var artifactsDeleted = 0
    private var failures = 0

    /**
     * Processes every candidate run once. Failed operations are counted, not thrown, so one
     * refused deletion does not stop the rest.
     *
     * @return the counts of this pass, including the number of failed operations
     */
    fun run(): MaintenanceSummary {
        log.info("Maintenance for {}/{}: retentionDays={}, keepMinimumRuns={}, dryRun={}", settings.owner, settings.repo, settings.retentionDays, settings.keepMinimumRuns, settings.dryRun)

        val runsToKeep = api.listWorkflowRuns(limit = settings.keepMinimumRuns).map { it.id }.toSet()
        log.info("Keeping the {} newest workflow runs: {}", runsToKeep.size, runsToKeep)

        val cutoff = LocalDate.now(clock).minusDays(settings.retentionDays.toLong())
        val candidates = settings.workflowRunFilters()
            .flatMap { api.listWorkflowRuns(it.copy(createdOnOrBefore = cutoff)) }
            .distinctBy { it.id }
            .filterNot { it.id in runsToKeep }
            .filterNot { it.id == settings.currentRunId }
            .filterNot { settings.keepPullRequests && it.isPullRequestRun }
            .filter { settings.statuses.isNotEmpty() || it.isCompleted }
        log.info("Found {} workflow runs created on or before {} to process", candidates.size, cutoff)

        candidates.forEach(::process)

        val summary = MaintenanceSummary(settings.dryRun, candidates.size, runsDeleted, logsDeleted, artifactsDeleted, failures)
        log.info("Summary: {} {} runs, {} logs, {} artifacts; failures={}", summary.verb, summary.runsDeleted, summary.logsDeleted, summary.artifactsDeleted, summary.failures)
        return summary
    }

    private fun process(run: WorkflowRun) {
        log.info("Workflow run {} [{}, {}, {}/{}, created {}]", run.id, run.name, run.event, run.status, run.conclusion, run.createdAt)
        if (settings.deleteLogs) {
            mutate("logs of workflow run ${run.id}", { api.deleteWorkflowRunLogs(run.id) }) { logsDeleted++ }
        }
        if (settings.deleteArtifacts) {
            attempt("list artifacts of workflow run ${run.id}") { api.listArtifacts(run.id) }?.forEach { artifact ->
                mutate("artifact ${artifact.id} (${artifact.name}) of workflow run ${run.id}", { api.deleteArtifact(artifact.id) }) { artifactsDeleted++ }
            }
        }
        if (settings.deletesEntireRuns) {
            mutate("workflow run ${run.id}", { api.deleteWorkflowRun(run.id) }) { runsDeleted++ }
        }
    }

    private fun mutate(subject: String, action: () -> Unit, onDeleted: () -> Unit) {
        if (settings.dryRun) {
            log.info("Dry run: would delete {}", subject)
            onDeleted()
            return
        }
        attempt("delete $subject") {
            action()
            log.info("Deleted {}", subject)
            onDeleted()
        }
        pause(MUTATION_PAUSE)
    }

    private fun <T> attempt(description: String, block: () -> T): T? = runCatching(block).getOrElse {
        failures++
        log.warn("Failed to {}: {}", description, it.message)
        null
    }

    private companion object {
        /** GitHub asks for at least one second between mutating requests to avoid secondary rate limits. */
        val MUTATION_PAUSE: Duration = Duration.ofSeconds(1)
    }
}
