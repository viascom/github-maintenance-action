package io.viascom.github.action.maintenance.core

import ch.tutteli.atrium.api.infix.en_GB.messageToContain
import ch.tutteli.atrium.api.infix.en_GB.toEqual
import ch.tutteli.atrium.api.infix.en_GB.toThrow
import ch.tutteli.atrium.api.infix.en_GB.values
import ch.tutteli.atrium.api.verbs.expect
import io.viascom.github.action.maintenance.api.WorkflowRunFilter
import io.viascom.github.action.maintenance.model.WorkflowRunStatus
import org.junit.jupiter.api.Test

class SettingsTest {

    private val minimal = mapOf(
        "INPUT_GITHUB_TOKEN" to "ghs_invented",
        "INPUT_REPOSITORY" to "viascom/github-maintenance-action",
    )

    /** Requirement pinned: every input arrives as INPUT_<NAME>, is trimmed, and lists split on commas. */
    @Test
    fun `reads every input from the environment`() {
        val settings = Settings.fromEnvironment(
            minimal + mapOf(
                "INPUT_GITHUB_BASE_URL" to " https://github.example.test/api/v3/ ",
                "INPUT_RETENTION_DAYS" to "10",
                "INPUT_KEEP_MINIMUM_RUNS" to "2",
                "INPUT_DELETE_LOGS" to "true",
                "INPUT_DELETE_ARTIFACTS" to "TRUE",
                "INPUT_ACTORS" to "nik-sta, itsmefox,,",
                "INPUT_BRANCHES" to "main,develop",
                "INPUT_EVENTS" to "Push, workflow_dispatch",
                "INPUT_STATUSES" to "failure, Cancelled",
                "INPUT_KEEP_PULL_REQUESTS" to "true",
                "INPUT_DRY_RUN" to "true",
                "INPUT_DEBUG" to "true",
                "GITHUB_RUN_ID" to "4242",
            ),
        )

        expect(settings) toEqual Settings(
            gitHubToken = "ghs_invented",
            gitHubBaseUrl = "https://github.example.test/api/v3",
            owner = "viascom",
            repo = "github-maintenance-action",
            retentionDays = 10,
            keepMinimumRuns = 2,
            deleteLogs = true,
            deleteArtifacts = true,
            actors = listOf("nik-sta", "itsmefox"),
            branches = listOf("main", "develop"),
            events = listOf("push", "workflow_dispatch"),
            statuses = listOf(WorkflowRunStatus.FAILURE, WorkflowRunStatus.CANCELLED),
            keepPullRequests = true,
            dryRun = true,
            debug = true,
            currentRunId = 4242,
        )
    }

    /** Requirement pinned: only the token and the repository are required; everything else has the documented default. */
    @Test
    fun `applies defaults when optional inputs are absent or blank`() {
        val settings = Settings.fromEnvironment(minimal + mapOf("INPUT_RETENTION_DAYS" to "  ", "INPUT_ACTORS" to ""))

        expect(settings.gitHubBaseUrl) toEqual "https://api.github.com"
        expect(settings.retentionDays) toEqual 31
        expect(settings.keepMinimumRuns) toEqual 5
        expect(settings.deleteLogs) toEqual false
        expect(settings.deleteArtifacts) toEqual false
        expect(settings.deletesEntireRuns) toEqual true
        expect(settings.actors) toEqual emptyList()
        expect(settings.statuses) toEqual emptyList()
        expect(settings.keepPullRequests) toEqual false
        expect(settings.dryRun) toEqual false
        expect(settings.debug) toEqual false
        expect(settings.currentRunId) toEqual null
    }

    /** Requirement pinned: a missing required input names itself. */
    @Test
    fun `refuses a missing token or repository`() {
        expect { Settings.fromEnvironment(minimal - "INPUT_GITHUB_TOKEN") }
            .toThrow<IllegalArgumentException> { messageToContain("input 'github_token' is required") }
        expect { Settings.fromEnvironment(minimal + ("INPUT_REPOSITORY" to " ")) }
            .toThrow<IllegalArgumentException> { messageToContain("input 'repository' is required") }
    }

    /** Requirement pinned: the repository is always owner/repo. */
    @Test
    fun `refuses a repository that is not owner slash repo`() {
        listOf("just-a-name", "/repo", "owner/").forEach { repository ->
            expect { Settings.fromEnvironment(minimal + ("INPUT_REPOSITORY" to repository)) }
                .toThrow<IllegalArgumentException> { messageToContain("must be in the format 'owner/repo'") }
        }
    }

    /** Requirement pinned: numbers must be whole and not negative, so a typo cannot widen a deletion. */
    @Test
    fun `refuses invalid numbers`() {
        expect { Settings.fromEnvironment(minimal + ("INPUT_RETENTION_DAYS" to "-1")) }
            .toThrow<IllegalArgumentException> { messageToContain("input 'retention_days' must be a whole number >= 0, got '-1'") }
        expect { Settings.fromEnvironment(minimal + ("INPUT_KEEP_MINIMUM_RUNS" to "five")) }
            .toThrow<IllegalArgumentException> { messageToContain("input 'keep_minimum_runs' must be a whole number >= 0, got 'five'") }
    }

    /** Requirement pinned: a boolean input is exactly true or false, in any case. */
    @Test
    fun `refuses a boolean that is neither true nor false`() {
        expect { Settings.fromEnvironment(minimal + ("INPUT_DRY_RUN" to "yes")) }
            .toThrow<IllegalArgumentException> { messageToContain("input 'dry_run' must be 'true' or 'false', got 'yes'") }
    }

    /** Requirement pinned: an unknown status fails fast instead of silently removing the filter. */
    @Test
    fun `refuses an unknown status and lists the allowed ones`() {
        expect { Settings.fromEnvironment(minimal + ("INPUT_STATUSES" to "failed")) }
            .toThrow<IllegalArgumentException> { messageToContain(values("unknown workflow run status 'failed'", "completed, action_required")) }
    }

    /** Requirement pinned: without filters there is exactly one unfiltered listing. */
    @Test
    fun `builds a single empty filter when no filter input is set`() {
        expect(Settings.fromEnvironment(minimal).workflowRunFilters()) toEqual listOf(WorkflowRunFilter())
    }

    /** Requirement pinned: GitHub takes one value per filter, so the inputs expand to their cartesian product. */
    @Test
    fun `builds one filter per combination of actors branches events and statuses`() {
        val settings = Settings.fromEnvironment(
            minimal + mapOf("INPUT_ACTORS" to "a1,a2", "INPUT_BRANCHES" to "main", "INPUT_STATUSES" to "success,failure"),
        )

        expect(settings.workflowRunFilters()) toEqual listOf(
            WorkflowRunFilter(actor = "a1", branch = "main", status = WorkflowRunStatus.SUCCESS),
            WorkflowRunFilter(actor = "a1", branch = "main", status = WorkflowRunStatus.FAILURE),
            WorkflowRunFilter(actor = "a2", branch = "main", status = WorkflowRunStatus.SUCCESS),
            WorkflowRunFilter(actor = "a2", branch = "main", status = WorkflowRunStatus.FAILURE),
        )
    }
}
