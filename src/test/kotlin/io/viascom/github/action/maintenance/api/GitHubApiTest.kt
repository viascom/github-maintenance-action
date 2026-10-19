package io.viascom.github.action.maintenance.api

import ch.tutteli.atrium.api.infix.en_GB.messageToContain
import ch.tutteli.atrium.api.infix.en_GB.toEqual
import ch.tutteli.atrium.api.infix.en_GB.toThrow
import ch.tutteli.atrium.api.infix.en_GB.values
import ch.tutteli.atrium.api.verbs.expect
import io.viascom.github.action.maintenance.model.WorkflowRunStatus
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.junit5.StartStop
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class GitHubApiTest {

    @StartStop
    private val server = MockWebServer()

    private val sleeps = mutableListOf<Duration>()
    private val clock = Clock.fixed(Instant.parse("2026-10-19T10:00:00Z"), ZoneOffset.UTC)

    private val api by lazy {
        GitHubApi(
            baseUrl = server.url("/").toString(),
            token = "ghs_invented",
            owner = "viascom",
            repo = "repo",
            rateLimitInterceptor = RateLimitInterceptor(clock, sleep = { sleeps += it }),
        )
    }

    private fun runsPage(total: Int, ids: LongRange) =
        """{"total_count":$total,"workflow_runs":[${ids.joinToString { """{"id":$it,"name":"CI","event":"push","status":"completed"}""" }}]}"""

    private fun artifactsPage(total: Int, ids: LongRange) =
        """{"total_count":$total,"artifacts":[${ids.joinToString { """{"id":$it,"name":"a$it","size_in_bytes":10,"expired":false}""" }}]}"""

    private fun json(body: String) = MockResponse.Builder().code(200).body(body).build()

    private fun noContent() = MockResponse.Builder().code(204).build()

    /** Requirement pinned: every request authenticates and declares the API version and a user agent, as GitHub requires. */
    @Test
    fun `sends the GitHub headers on every request`() {
        server.enqueue(json(runsPage(0, LongRange.EMPTY)))

        api.listWorkflowRuns()

        val request = server.takeRequest()
        expect(request.headers["Authorization"]) toEqual "Bearer ghs_invented"
        expect(request.headers["Accept"]) toEqual "application/vnd.github+json"
        expect(request.headers["X-GitHub-Api-Version"]) toEqual "2022-11-28"
        expect(request.headers["User-Agent"]) toEqual "viascom-github-maintenance-action"
    }

    /** Requirement pinned: the filter maps onto GitHub's query parameters, with the status in GitHub's lower-case spelling. */
    @Test
    fun `maps the filter onto query parameters`() {
        server.enqueue(json(runsPage(0, LongRange.EMPTY)))

        api.listWorkflowRuns(
            WorkflowRunFilter(
                actor = "nik-sta",
                branch = "main",
                event = "push",
                status = WorkflowRunStatus.TIMED_OUT,
                createdOnOrBefore = LocalDate.of(2026, 9, 19),
            ),
        )

        val url = server.takeRequest().url
        expect(url.encodedPath) toEqual "/repos/viascom/repo/actions/runs"
        expect(url.queryParameter("per_page")) toEqual "100"
        expect(url.queryParameter("page")) toEqual "1"
        expect(url.queryParameter("exclude_pull_requests")) toEqual "true"
        expect(url.queryParameter("actor")) toEqual "nik-sta"
        expect(url.queryParameter("branch")) toEqual "main"
        expect(url.queryParameter("event")) toEqual "push"
        expect(url.queryParameter("status")) toEqual "timed_out"
        expect(url.queryParameter("created")) toEqual "*..2026-09-19"
    }

    /** Requirement pinned: runs are fetched 100 at a time until total_count is reached. */
    @Test
    fun `pages through all workflow runs`() {
        server.enqueue(json(runsPage(250, 1L..100L)))
        server.enqueue(json(runsPage(250, 101L..200L)))
        server.enqueue(json(runsPage(250, 201L..250L)))

        val runs = api.listWorkflowRuns()

        expect(runs.map { it.id }) toEqual (1L..250L).toList()
        expect(server.requestCount) toEqual 3
        expect(server.takeRequest().url.queryParameter("page")) toEqual "1"
        expect(server.takeRequest().url.queryParameter("page")) toEqual "2"
        expect(server.takeRequest().url.queryParameter("page")) toEqual "3"
    }

    /** Requirement pinned: an empty page ends the listing even if total_count promised more. */
    @Test
    fun `stops at an empty page`() {
        server.enqueue(json(runsPage(500, 1L..100L)))
        server.enqueue(json(runsPage(500, LongRange.EMPTY)))

        expect(api.listWorkflowRuns().size) toEqual 100
        expect(server.requestCount) toEqual 2
    }

    /** Requirement pinned: a small limit asks GitHub for exactly that many runs in one page. */
    @Test
    fun `requests only as many runs as the limit`() {
        server.enqueue(json(runsPage(5000, 1L..3L)))

        val runs = api.listWorkflowRuns(limit = 3)

        expect(runs.map { it.id }) toEqual listOf(1L, 2L, 3L)
        expect(server.requestCount) toEqual 1
        expect(server.takeRequest().url.queryParameter("per_page")) toEqual "3"
    }

    /** Requirement pinned: a limit of zero makes no request at all. */
    @Test
    fun `makes no request for a limit of zero`() {
        expect(api.listWorkflowRuns(limit = 0)) toEqual emptyList()
        expect(server.requestCount) toEqual 0
    }

    /** Requirement pinned: GitHub caps a filtered listing at 1000 results, so the client stops after ten full pages. */
    @Test
    fun `stops at GitHub's search result cap`() {
        repeat(10) { page -> server.enqueue(json(runsPage(5000, (page * 100L + 1)..(page * 100L + 100)))) }

        expect(api.listWorkflowRuns().size) toEqual 1000
        expect(server.requestCount) toEqual 10
    }

    /** Requirement pinned: artifacts of one run are listed completely, 100 per page. */
    @Test
    fun `pages through the artifacts of a run`() {
        server.enqueue(json(artifactsPage(120, 1L..100L)))
        server.enqueue(json(artifactsPage(120, 101L..120L)))

        val artifacts = api.listArtifacts(77)

        expect(artifacts.size) toEqual 120
        expect(artifacts.first().name) toEqual "a1"
        val first = server.takeRequest().url
        expect(first.encodedPath) toEqual "/repos/viascom/repo/actions/runs/77/artifacts"
        expect(first.queryParameter("per_page")) toEqual "100"
    }

    /** Requirement pinned: the three deletions hit GitHub's documented DELETE endpoints. */
    @Test
    fun `deletes runs logs and artifacts on their endpoints`() {
        repeat(3) { server.enqueue(noContent()) }

        api.deleteWorkflowRun(11)
        api.deleteWorkflowRunLogs(12)
        api.deleteArtifact(13)

        val requests = List(3) { server.takeRequest() }
        expect(requests.map { it.method }) toEqual listOf("DELETE", "DELETE", "DELETE")
        expect(requests.map { it.url.encodedPath }) toEqual listOf(
            "/repos/viascom/repo/actions/runs/11",
            "/repos/viascom/repo/actions/runs/12/logs",
            "/repos/viascom/repo/actions/artifacts/13",
        )
    }

    /** Requirement pinned: a non-2xx answer is an error that names the request, the status and GitHub's message. */
    @Test
    fun `fails on an unsuccessful response`() {
        server.enqueue(MockResponse.Builder().code(404).body("""{"message":"Not Found"}""").build())

        expect { api.listArtifacts(5) }
            .toThrow<GitHubApiException> { messageToContain(values("GET", "/actions/runs/5/artifacts", "HTTP 404", "Not Found")) }
    }

    /** Requirement pinned: a forbidden deletion explains the permission the token is missing. */
    @Test
    fun `explains a forbidden deletion`() {
        server.enqueue(MockResponse.Builder().code(403).body("""{"message":"Resource not accessible by integration"}""").build())

        expect { api.deleteWorkflowRun(5) }
            .toThrow<GitHubApiException> { messageToContain(values("HTTP 403", "actions: write", "Resource not accessible")) }
    }

    /** Requirement pinned: a rate-limited deletion waits as instructed and is retried, so the caller sees only the final success. */
    @Test
    fun `retries after a rate limit answer`() {
        server.enqueue(MockResponse.Builder().code(429).addHeader("retry-after", "7").body("slow down").build())
        server.enqueue(noContent())

        api.deleteWorkflowRun(5)

        expect(server.requestCount) toEqual 2
        expect(sleeps.toList()) toEqual listOf(Duration.ofSeconds(7))
    }
}
