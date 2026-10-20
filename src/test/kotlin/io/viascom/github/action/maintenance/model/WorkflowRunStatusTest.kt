package io.viascom.github.action.maintenance.model

import ch.tutteli.atrium.api.infix.en_GB.messageToContain
import ch.tutteli.atrium.api.infix.en_GB.toEqual
import ch.tutteli.atrium.api.infix.en_GB.toThrow
import ch.tutteli.atrium.api.infix.en_GB.values
import ch.tutteli.atrium.api.verbs.expect
import org.junit.jupiter.api.Test

class WorkflowRunStatusTest {

    /** Requirement pinned: the input accepts GitHub's spelling in any case and surrounding whitespace. */
    @Test
    fun `parses GitHub's status spelling case-insensitively`() {
        expect(WorkflowRunStatus.parse(" Timed_Out ")) toEqual WorkflowRunStatus.TIMED_OUT
        expect(WorkflowRunStatus.TIMED_OUT.apiValue) toEqual "timed_out"
    }

    /** Requirement pinned: a value GitHub does not know is refused with the list of accepted ones. */
    @Test
    fun `refuses an unknown status`() {
        expect { WorkflowRunStatus.parse("done") }
            .toThrow<IllegalArgumentException> { messageToContain(values("unknown workflow run status 'done'", "allowed: completed")) }
    }

    /** Requirement pinned: pull request runs are recognised by their triggering event. */
    @Test
    fun `recognises pull request runs by event`() {
        expect(WorkflowRun(id = 1, event = "pull_request").isPullRequestRun) toEqual true
        expect(WorkflowRun(id = 2, event = "pull_request_target").isPullRequestRun) toEqual true
        expect(WorkflowRun(id = 3, event = "push").isPullRequestRun) toEqual false
    }
}
