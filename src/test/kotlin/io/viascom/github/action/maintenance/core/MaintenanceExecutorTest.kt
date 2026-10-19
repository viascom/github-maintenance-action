package io.viascom.github.action.maintenance.core

import ch.tutteli.atrium.api.infix.en_GB.toEqual
import ch.tutteli.atrium.api.verbs.expect
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import io.viascom.github.action.maintenance.api.GitHubApi
import io.viascom.github.action.maintenance.api.GitHubApiException
import io.viascom.github.action.maintenance.api.WorkflowRunFilter
import io.viascom.github.action.maintenance.model.Artifact
import io.viascom.github.action.maintenance.model.WorkflowRun
import io.viascom.github.action.maintenance.model.WorkflowRunStatus
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class MaintenanceExecutorTest {

    private val api = mockk<GitHubApi>()
    private val clock = Clock.fixed(Instant.parse("2026-10-19T10:00:00Z"), ZoneOffset.UTC)
    private val pauses = mutableListOf<Duration>()
    private val cutoff: LocalDate = LocalDate.of(2026, 9, 19)

    private val settings = Settings(
        gitHubToken = "ghs_invented",
        gitHubBaseUrl = "https://api.github.com",
        owner = "viascom",
        repo = "repo",
        retentionDays = 30,
        keepMinimumRuns = 2,
        deleteLogs = false,
        deleteArtifacts = false,
        actors = emptyList(),
        branches = emptyList(),
        events = emptyList(),
        statuses = emptyList(),
        keepPullRequests = false,
        dryRun = false,
        debug = false,
        currentRunId = 999,
    )

    private fun executor(settings: Settings = this.settings) = MaintenanceExecutor(api, settings, clock) { pauses += it }

    private fun run(id: Long, event: String = "push") = WorkflowRun(id = id, name = "CI", event = event, status = "completed")

    private fun stubListings(newest: List<WorkflowRun>, old: Map<WorkflowRunFilter, List<WorkflowRun>>, keep: Int = 2) {
        every { api.listWorkflowRuns(WorkflowRunFilter(), keep) } returns newest
        old.forEach { (filter, runs) -> every { api.listWorkflowRuns(filter.copy(createdOnOrBefore = cutoff)) } returns runs }
        every { api.deleteWorkflowRun(any()) } just Runs
        every { api.deleteWorkflowRunLogs(any()) } just Runs
        every { api.deleteArtifact(any()) } just Runs
    }

    /** Requirement pinned: the newest keep_minimum_runs runs survive even when they fall inside the retention window. */
    @Test
    fun `deletes old runs except the newest ones to keep`() {
        stubListings(newest = listOf(run(1), run(2)), old = mapOf(WorkflowRunFilter() to listOf(run(2), run(3), run(4))))

        val summary = executor().run()

        verify(exactly = 0) { api.deleteWorkflowRun(2) }
        verify { api.deleteWorkflowRun(3) }
        verify { api.deleteWorkflowRun(4) }
        expect(summary) toEqual MaintenanceSummary(dryRun = false, candidates = 2, runsDeleted = 2, logsDeleted = 0, artifactsDeleted = 0, failures = 0)
        expect(pauses.toList()) toEqual listOf(Duration.ofSeconds(1), Duration.ofSeconds(1))
    }

    /** Requirement pinned: the cutoff is today minus retention_days, in UTC, and is handed to GitHub as the created filter. */
    @Test
    fun `asks GitHub for runs created on or before the retention cutoff`() {
        every { api.listWorkflowRuns(WorkflowRunFilter(), 2) } returns emptyList()
        every { api.listWorkflowRuns(WorkflowRunFilter(createdOnOrBefore = LocalDate.of(2026, 10, 19))) } returns emptyList()

        executor(settings.copy(retentionDays = 0)).run()

        verify { api.listWorkflowRuns(WorkflowRunFilter(createdOnOrBefore = LocalDate.of(2026, 10, 19))) }
    }

    /** Requirement pinned: delete_logs alone removes logs and leaves the run in place. */
    @Test
    fun `deletes only logs when delete_logs is set`() {
        stubListings(newest = emptyList(), old = mapOf(WorkflowRunFilter() to listOf(run(3))))

        val summary = executor(settings.copy(deleteLogs = true)).run()

        verify { api.deleteWorkflowRunLogs(3) }
        verify(exactly = 0) { api.deleteWorkflowRun(any()) }
        expect(summary.logsDeleted) toEqual 1
        expect(summary.runsDeleted) toEqual 0
    }

    /** Requirement pinned: delete_artifacts alone removes every artifact of the run, paging through them, and leaves the run in place. */
    @Test
    fun `deletes only artifacts when delete_artifacts is set`() {
        stubListings(newest = emptyList(), old = mapOf(WorkflowRunFilter() to listOf(run(3))))
        every { api.listArtifacts(3) } returns listOf(Artifact(id = 31, name = "report"), Artifact(id = 32, name = "coverage"))

        val summary = executor(settings.copy(deleteArtifacts = true)).run()

        verify { api.deleteArtifact(31) }
        verify { api.deleteArtifact(32) }
        verify(exactly = 0) { api.deleteWorkflowRun(any()) }
        verify(exactly = 0) { api.deleteWorkflowRunLogs(any()) }
        expect(summary.artifactsDeleted) toEqual 2
    }

    /** Requirement pinned: both flags together clean logs and artifacts without deleting the run. */
    @Test
    fun `deletes logs and artifacts but not the run when both flags are set`() {
        stubListings(newest = emptyList(), old = mapOf(WorkflowRunFilter() to listOf(run(3))))
        every { api.listArtifacts(3) } returns listOf(Artifact(id = 31))

        val summary = executor(settings.copy(deleteLogs = true, deleteArtifacts = true)).run()

        verify { api.deleteWorkflowRunLogs(3) }
        verify { api.deleteArtifact(31) }
        verify(exactly = 0) { api.deleteWorkflowRun(any()) }
        expect(summary) toEqual MaintenanceSummary(dryRun = false, candidates = 1, runsDeleted = 0, logsDeleted = 1, artifactsDeleted = 1, failures = 0)
    }

    /** Requirement pinned: keep_pull_requests keeps runs triggered by pull_request and pull_request_target. */
    @Test
    fun `keeps pull request runs when asked to`() {
        stubListings(
            newest = emptyList(),
            old = mapOf(WorkflowRunFilter() to listOf(run(3, event = "pull_request"), run(4, event = "pull_request_target"), run(5))),
        )

        executor(settings.copy(keepPullRequests = true)).run()

        verify(exactly = 0) { api.deleteWorkflowRun(3) }
        verify(exactly = 0) { api.deleteWorkflowRun(4) }
        verify { api.deleteWorkflowRun(5) }
    }

    /** Requirement pinned: the run executing the action is never a candidate, whatever the retention says. */
    @Test
    fun `never deletes its own workflow run`() {
        stubListings(newest = emptyList(), old = mapOf(WorkflowRunFilter() to listOf(run(999), run(5))))

        executor().run()

        verify(exactly = 0) { api.deleteWorkflowRun(999) }
        verify { api.deleteWorkflowRun(5) }
    }

    /** Requirement pinned: a run matched by several filter combinations is processed once. */
    @Test
    fun `merges the listings of all filter combinations without duplicates`() {
        val success = WorkflowRunFilter(status = WorkflowRunStatus.SUCCESS)
        val failure = WorkflowRunFilter(status = WorkflowRunStatus.FAILURE)
        stubListings(newest = emptyList(), old = mapOf(success to listOf(run(3), run(4)), failure to listOf(run(4), run(5))))

        val summary = executor(settings.copy(statuses = listOf(WorkflowRunStatus.SUCCESS, WorkflowRunStatus.FAILURE))).run()

        verify(exactly = 1) { api.deleteWorkflowRun(4) }
        expect(summary.candidates) toEqual 3
        expect(summary.runsDeleted) toEqual 3
    }

    /** Requirement pinned: a dry run calls no mutating endpoint, does not pause, and still reports the counts. */
    @Test
    fun `performs no deletion in a dry run`() {
        stubListings(newest = emptyList(), old = mapOf(WorkflowRunFilter() to listOf(run(3), run(4))))
        every { api.listArtifacts(any()) } returns listOf(Artifact(id = 31))

        val summary = executor(settings.copy(dryRun = true, deleteLogs = true, deleteArtifacts = true)).run()

        verify(exactly = 0) { api.deleteWorkflowRun(any()) }
        verify(exactly = 0) { api.deleteWorkflowRunLogs(any()) }
        verify(exactly = 0) { api.deleteArtifact(any()) }
        expect(summary) toEqual MaintenanceSummary(dryRun = true, candidates = 2, runsDeleted = 0, logsDeleted = 2, artifactsDeleted = 2, failures = 0)
        expect(pauses.toList()) toEqual emptyList()
    }

    /** Requirement pinned: one failed deletion is counted and does not stop the remaining runs from being processed. */
    @Test
    fun `counts failures and continues with the next run`() {
        stubListings(newest = emptyList(), old = mapOf(WorkflowRunFilter() to listOf(run(3), run(4))))
        every { api.deleteWorkflowRun(3) } throws GitHubApiException("DELETE", "https://api.github.com/x", 403, "Resource not accessible")

        val summary = executor().run()

        verify { api.deleteWorkflowRun(4) }
        expect(summary.failures) toEqual 1
        expect(summary.runsDeleted) toEqual 1
    }

    /** Requirement pinned: when the artifact listing itself fails, the run's artifacts are skipped and the failure is counted. */
    @Test
    fun `counts a failed artifact listing as one failure`() {
        stubListings(newest = emptyList(), old = mapOf(WorkflowRunFilter() to listOf(run(3))))
        every { api.listArtifacts(3) } throws GitHubApiException("GET", "https://api.github.com/x", 500, "boom")

        val summary = executor(settings.copy(deleteArtifacts = true)).run()

        verify(exactly = 0) { api.deleteArtifact(any()) }
        expect(summary.failures) toEqual 1
    }

    /** Requirement pinned: keep_minimum_runs 0 keeps nothing and still works. */
    @Test
    fun `keeps nothing when keep_minimum_runs is zero`() {
        stubListings(newest = emptyList(), old = mapOf(WorkflowRunFilter() to listOf(run(1))), keep = 0)

        executor(settings.copy(keepMinimumRuns = 0)).run()

        verify { api.deleteWorkflowRun(1) }
    }
}
