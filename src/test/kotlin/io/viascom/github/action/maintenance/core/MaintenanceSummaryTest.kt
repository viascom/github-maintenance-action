package io.viascom.github.action.maintenance.core

import ch.tutteli.atrium.api.infix.en_GB.messageToContain
import ch.tutteli.atrium.api.infix.en_GB.toEqual
import ch.tutteli.atrium.api.infix.en_GB.toThrow
import ch.tutteli.atrium.api.verbs.expect
import org.junit.jupiter.api.Test

class MaintenanceSummaryTest {

    /** Requirement pinned: a dry run reports what would happen, a real run what did happen. */
    @Test
    fun `verb follows the dry run flag`() {
        expect(summary(dryRun = true).verb) toEqual "would delete"
        expect(summary(dryRun = false).verb) toEqual "deleted"
    }

    /** Requirement pinned: any failed operation fails the action, after everything else was processed. */
    @Test
    fun `requireNoFailures throws only when an operation failed`() {
        summary(failures = 0).requireNoFailures()

        expect { summary(failures = 2).requireNoFailures() }
            .toThrow<IllegalStateException> { messageToContain("2 of the maintenance operations failed") }
    }

    private fun summary(dryRun: Boolean = false, failures: Int = 0) =
        MaintenanceSummary(dryRun = dryRun, candidates = 3, runsDeleted = 1, logsDeleted = 1, artifactsDeleted = 1, failures = failures)
}
