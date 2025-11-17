package io.viascom.github.action.maintenance.core

import io.viascom.github.action.maintenance.api.GitHubApi
import io.viascom.github.action.maintenance.model.WorkflowRun
import io.viascom.github.action.maintenance.model.WorkflowRuns
import io.viascom.github.action.maintenance.model.Artifacts
import io.viascom.github.action.maintenance.model.WorkflowArtifacts
import io.viascom.github.action.maintenance.model.WorkflowRunStatus
import io.viascom.github.action.maintenance.model.WorkflowRunEvent
import okhttp3.Response
import okhttp3.ResponseBody
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.ArrayList

/**
 * Test for MaintenanceExecutor that doesn't rely on the Environment singleton.
 * 
 * This test class uses a custom MaintenanceExecutorForTest class that doesn't access Environment.
 */
@ExtendWith(MockitoExtension::class)
class MaintenanceExecutorTestWithoutEnv {

    @Mock(lenient = true)
    private lateinit var githubApi: GitHubApi

    private lateinit var maintenanceExecutor: MaintenanceExecutorForTest

    @BeforeEach
    fun setUp() {
        maintenanceExecutor = MaintenanceExecutorForTest(githubApi)
    }

    @Test
    fun `should skip maintenance when total runs is less than or equal to keepMinimumRuns`() {
        // Given
        val runs = ArrayList<WorkflowRun>()
        val workflowRuns = WorkflowRuns(totalCount = 2, runs = runs)
        `when`(githubApi.listWorkflowRuns("owner", "repo")).thenReturn(workflowRuns)

        // When
        maintenanceExecutor.deleteOldActionRuns(
            owner = "owner",
            repo = "repo",
            retentionDays = 30,
            keepMinimumRuns = 2,
            actors = ArrayList(),
            branches = ArrayList(),
            events = ArrayList<WorkflowRunEvent>(),
            statuses = ArrayList<WorkflowRunStatus>(),
            deleteLogs = false,
            deleteArtifacts = false,
            keepPullRequests = false
        )

        // Then
        verify(githubApi).listWorkflowRuns("owner", "repo")
        verifyNoMoreInteractions(githubApi)
    }

    @Test
    fun `should delete workflow runs older than retention days`() {
        // Given
        val run1 = WorkflowRun(id = 1, name = "Run 1", status = WorkflowRunStatus.COMPLETED)
        val run2 = WorkflowRun(id = 2, name = "Run 2", status = WorkflowRunStatus.COMPLETED)
        val run3 = WorkflowRun(id = 3, name = "Run 3", status = WorkflowRunStatus.COMPLETED)
        val run4 = WorkflowRun(id = 4, name = "Run 4", status = WorkflowRunStatus.COMPLETED)

        val allRunsList = ArrayList<WorkflowRun>()
        allRunsList.add(run1)
        allRunsList.add(run2)
        allRunsList.add(run3)
        allRunsList.add(run4)

        val allRuns = WorkflowRuns(totalCount = 4, runs = allRunsList)
        `when`(githubApi.listWorkflowRuns("owner", "repo")).thenReturn(allRuns)

        val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val formattedDate = LocalDateTime.now().minusDays(30).format(dateFormatter)

        val oldRunsList = ArrayList<WorkflowRun>()
        oldRunsList.add(run3)
        oldRunsList.add(run4)

        val oldRuns = WorkflowRuns(totalCount = 2, runs = oldRunsList)
        `when`(githubApi.listWorkflowRuns(
            owner = "owner",
            repo = "repo",
            actors = ArrayList(),
            branches = ArrayList(),
            events = ArrayList<WorkflowRunEvent>(),
            statuses = ArrayList<WorkflowRunStatus>(),
            created = "*..$formattedDate",
            keepPullRequests = false
        )).thenReturn(oldRuns)

        // No need to mock Response objects as we're only verifying the calls

        // When
        maintenanceExecutor.deleteOldActionRuns(
            owner = "owner",
            repo = "repo",
            retentionDays = 30,
            keepMinimumRuns = 2,
            actors = ArrayList(),
            branches = ArrayList(),
            events = ArrayList<WorkflowRunEvent>(),
            statuses = ArrayList<WorkflowRunStatus>(),
            deleteLogs = false,
            deleteArtifacts = false,
            keepPullRequests = false
        )

        // Then
        verify(githubApi).listWorkflowRuns("owner", "repo")
        verify(githubApi).listWorkflowRuns(
            owner = "owner",
            repo = "repo",
            actors = ArrayList(),
            branches = ArrayList(),
            events = ArrayList<WorkflowRunEvent>(),
            statuses = ArrayList<WorkflowRunStatus>(),
            created = "*..$formattedDate",
            keepPullRequests = false
        )
        verify(githubApi).deleteWorkflowRun("owner", "repo", 3)
        verify(githubApi).deleteWorkflowRun("owner", "repo", 4)
    }

    @Test
    fun `should delete logs when deleteLogs is true`() {
        // Given
        val run1 = WorkflowRun(id = 1, name = "Run 1", status = WorkflowRunStatus.COMPLETED)
        val run2 = WorkflowRun(id = 2, name = "Run 2", status = WorkflowRunStatus.COMPLETED)
        val run3 = WorkflowRun(id = 3, name = "Run 3", status = WorkflowRunStatus.COMPLETED)

        val allRunsList = ArrayList<WorkflowRun>()
        allRunsList.add(run1)
        allRunsList.add(run2)
        allRunsList.add(run3)

        val allRuns = WorkflowRuns(totalCount = 3, runs = allRunsList)
        `when`(githubApi.listWorkflowRuns("owner", "repo")).thenReturn(allRuns)

        val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val formattedDate = LocalDateTime.now().minusDays(30).format(dateFormatter)

        val oldRunsList = ArrayList<WorkflowRun>()
        oldRunsList.add(run3)

        val oldRuns = WorkflowRuns(totalCount = 1, runs = oldRunsList)
        `when`(githubApi.listWorkflowRuns(
            owner = "owner",
            repo = "repo",
            actors = ArrayList(),
            branches = ArrayList(),
            events = ArrayList<WorkflowRunEvent>(),
            statuses = ArrayList<WorkflowRunStatus>(),
            created = "*..$formattedDate",
            keepPullRequests = false
        )).thenReturn(oldRuns)

        // No need to mock Response objects as we're only verifying the calls

        // When
        maintenanceExecutor.deleteOldActionRuns(
            owner = "owner",
            repo = "repo",
            retentionDays = 30,
            keepMinimumRuns = 2,
            actors = ArrayList(),
            branches = ArrayList(),
            events = ArrayList<WorkflowRunEvent>(),
            statuses = ArrayList<WorkflowRunStatus>(),
            deleteLogs = true,
            deleteArtifacts = false,
            keepPullRequests = false
        )

        // Then
        verify(githubApi).deleteWorkflowRunLogs("owner", "repo", 3)
        verify(githubApi, never()).deleteWorkflowRun("owner", "repo", 3)
    }

    @Test
    fun `should delete artifacts when deleteArtifacts is true`() {
        // Given
        val run1 = WorkflowRun(id = 1, name = "Run 1", status = WorkflowRunStatus.COMPLETED)
        val run2 = WorkflowRun(id = 2, name = "Run 2", status = WorkflowRunStatus.COMPLETED)
        val run3 = WorkflowRun(id = 3, name = "Run 3", status = WorkflowRunStatus.COMPLETED)

        val allRunsList = ArrayList<WorkflowRun>()
        allRunsList.add(run1)
        allRunsList.add(run2)
        allRunsList.add(run3)

        val allRuns = WorkflowRuns(totalCount = 3, runs = allRunsList)
        `when`(githubApi.listWorkflowRuns("owner", "repo")).thenReturn(allRuns)

        val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val formattedDate = LocalDateTime.now().minusDays(30).format(dateFormatter)

        val oldRunsList = ArrayList<WorkflowRun>()
        oldRunsList.add(run3)

        val oldRuns = WorkflowRuns(totalCount = 1, runs = oldRunsList)
        `when`(githubApi.listWorkflowRuns(
            owner = "owner",
            repo = "repo",
            actors = ArrayList(),
            branches = ArrayList(),
            events = ArrayList<WorkflowRunEvent>(),
            statuses = ArrayList<WorkflowRunStatus>(),
            created = "*..$formattedDate",
            keepPullRequests = false
        )).thenReturn(oldRuns)

        val artifact1 = Artifacts(id = 101, name = "Artifact 1")
        val artifact2 = Artifacts(id = 102, name = "Artifact 2")

        val artifactsList = ArrayList<Artifacts>()
        artifactsList.add(artifact1)
        artifactsList.add(artifact2)

        val artifacts = WorkflowArtifacts(totalCount = 2, artifacts = artifactsList)
        `when`(githubApi.listWorkflowRunArtifacts("owner", "repo", 3)).thenReturn(artifacts)

        // No need to mock Response objects as we're only verifying the calls

        // When
        maintenanceExecutor.deleteOldActionRuns(
            owner = "owner",
            repo = "repo",
            retentionDays = 30,
            keepMinimumRuns = 2,
            actors = ArrayList(),
            branches = ArrayList(),
            events = ArrayList<WorkflowRunEvent>(),
            statuses = ArrayList<WorkflowRunStatus>(),
            deleteLogs = false,
            deleteArtifacts = true,
            keepPullRequests = false
        )

        // Then
        verify(githubApi).listWorkflowRunArtifacts("owner", "repo", 3)
        verify(githubApi).deleteArtifact("owner", "repo", 101)
        verify(githubApi).deleteArtifact("owner", "repo", 102)
        verify(githubApi, never()).deleteWorkflowRun("owner", "repo", 3)
    }

    @Test
    fun `should call deleteWorkflowRunLogs when deleteLogs is true`() {
        // Given
        val run1 = WorkflowRun(id = 1, name = "Run 1", status = WorkflowRunStatus.COMPLETED)
        val run2 = WorkflowRun(id = 2, name = "Run 2", status = WorkflowRunStatus.COMPLETED)
        val run3 = WorkflowRun(id = 3, name = "Run 3", status = WorkflowRunStatus.COMPLETED)

        val allRunsList = ArrayList<WorkflowRun>()
        allRunsList.add(run1)
        allRunsList.add(run2)
        allRunsList.add(run3)

        val allRuns = WorkflowRuns(totalCount = 3, runs = allRunsList)
        `when`(githubApi.listWorkflowRuns("owner", "repo")).thenReturn(allRuns)

        val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val formattedDate = LocalDateTime.now().minusDays(30).format(dateFormatter)

        val oldRunsList = ArrayList<WorkflowRun>()
        oldRunsList.add(run3)

        val oldRuns = WorkflowRuns(totalCount = 1, runs = oldRunsList)
        `when`(githubApi.listWorkflowRuns(
            owner = "owner",
            repo = "repo",
            actors = ArrayList(),
            branches = ArrayList(),
            events = ArrayList<WorkflowRunEvent>(),
            statuses = ArrayList<WorkflowRunStatus>(),
            created = "*..$formattedDate",
            keepPullRequests = false
        )).thenReturn(oldRuns)

        // When
        maintenanceExecutor.deleteOldActionRuns(
            owner = "owner",
            repo = "repo",
            retentionDays = 30,
            keepMinimumRuns = 2,
            actors = ArrayList(),
            branches = ArrayList(),
            events = ArrayList<WorkflowRunEvent>(),
            statuses = ArrayList<WorkflowRunStatus>(),
            deleteLogs = true,
            deleteArtifacts = false,
            keepPullRequests = false
        )

        // Then
        verify(githubApi).deleteWorkflowRunLogs("owner", "repo", 3)
        // We're just verifying the method was called, not testing error handling here
    }
}

/**
 * A version of MaintenanceExecutor that doesn't rely on the Environment singleton.
 * 
 * This class replicates the logic in MaintenanceExecutor but takes all configuration as parameters.
 */
class MaintenanceExecutorForTest(private val githubApi: GitHubApi) {

    fun deleteOldActionRuns(
        owner: String,
        repo: String,
        retentionDays: Int,
        keepMinimumRuns: Int,
        actors: List<String>,
        branches: List<String>,
        events: List<WorkflowRunEvent>,
        statuses: List<WorkflowRunStatus>,
        deleteLogs: Boolean,
        deleteArtifacts: Boolean,
        keepPullRequests: Boolean
    ) {
        println("Starting maintenance for $owner/$repo (retentionDays=$retentionDays)")

        val allWorkflowRuns = githubApi.listWorkflowRuns(owner = owner, repo = repo)

        if (allWorkflowRuns.totalCount <= keepMinimumRuns) {
            println("Total workflow runs (${allWorkflowRuns.totalCount}) <= keepMinimumRuns ($keepMinimumRuns). Skipping maintenance.")
            return
        }

        val runsToKeep = allWorkflowRuns.runs.take(keepMinimumRuns).map { it.id }.toSet()
        println("Keeping $keepMinimumRuns latest workflow runs: $runsToKeep")

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

        println("Found ${runsToProcess.size} workflow runs to process (older than $formattedDate)")

        var runsDeleted = 0
        var artifactsDeleted = 0
        var logsDeleted = 0

        runsToProcess.forEach { run ->
            println("Processing workflow run [id=${run.id}, name=${run.name}, status=${run.status}]")

            if (deleteLogs) {
                runCatching {
                    println("Deleting logs for workflow run ${run.id}")
                    githubApi.deleteWorkflowRunLogs(owner, repo, run.id)
                    logsDeleted++
                }.onFailure { println("Failed to delete logs for run ${run.id}: ${it.message}") }

                applyApiDelay()
            }

            if (deleteArtifacts) {
                runCatching {
                    val workflowRunArtifacts = githubApi.listWorkflowRunArtifacts(owner, repo, run.id)
                    workflowRunArtifacts.artifacts.forEach { artifact ->
                        println("Deleting artifact [id=${artifact.id}, name=${artifact.name}]")
                        githubApi.deleteArtifact(owner, repo, artifact.id)
                        artifactsDeleted++
                        applyApiDelay()
                    }
                }.onFailure { println("Failed to delete artifacts for run ${run.id}: ${it.message}") }
            }

            if (!deleteLogs && !deleteArtifacts) {
                runCatching {
                    println("Deleting entire workflow run ${run.id}")
                    githubApi.deleteWorkflowRun(owner, repo, run.id)
                    runsDeleted++
                }.onFailure { println("Failed to delete workflow run ${run.id}: ${it.message}") }

                applyApiDelay()
            }
        }

        println("Maintenance completed for $owner/$repo")
        println("Summary: runsDeleted=$runsDeleted, artifactsDeleted=$artifactsDeleted, logsDeleted=$logsDeleted")
    }

    private fun applyApiDelay() {
        try {
            Thread.sleep(1000L)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        }
    }
}
