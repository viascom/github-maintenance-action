package io.viascom.github.action.maintenance.core

import ch.tutteli.atrium.api.infix.en_GB.toContain
import ch.tutteli.atrium.api.infix.en_GB.toEqual
import ch.tutteli.atrium.api.verbs.expect
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class ActionsReporterTest {

    private val summary = MaintenanceSummary(dryRun = false, candidates = 4, runsDeleted = 3, logsDeleted = 0, artifactsDeleted = 0, failures = 1)

    /** Requirement pinned: the counts are published as step outputs a workflow can read. */
    @Test
    fun `appends step outputs`(@TempDir dir: File) {
        val output = File(dir, "output").apply { writeText("existing=1\n") }

        ActionsReporter(mapOf("GITHUB_OUTPUT" to output.path)).publish(summary)

        expect(output.readText()) toEqual "existing=1\nruns_deleted=3\nlogs_deleted=0\nartifacts_deleted=0\nfailures=1\n"
    }

    /** Requirement pinned: the job summary shows the result as a table, labelled as a dry run when it was one. */
    @Test
    fun `appends a job summary table`(@TempDir dir: File) {
        val stepSummary = File(dir, "summary")

        ActionsReporter(mapOf("GITHUB_STEP_SUMMARY" to stepSummary.path)).publish(summary.copy(dryRun = true))

        expect(stepSummary.readText()) toContain "### GitHub Maintenance (dry run)"
        expect(stepSummary.readText()) toContain "| Workflow runs would delete | 3 |"
        expect(stepSummary.readText()) toContain "| Failures | 1 |"
    }

    /** Requirement pinned: outside a workflow nothing is written and nothing fails. */
    @Test
    fun `does nothing without the workflow files`() {
        ActionsReporter(emptyMap()).publish(summary)
    }
}
