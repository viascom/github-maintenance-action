package io.viascom.github.action.maintenance

import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Bucket
import io.viascom.github.action.maintenance.api.GitHubApi
import io.viascom.github.action.maintenance.core.Environment
import io.viascom.github.action.maintenance.core.MaintenanceExecutor
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import java.time.Duration
import java.time.Instant

@SpringBootApplication
open class GitHubActionApplication(
    private val githubApi: GitHubApi,
    private val maintenanceExecutor: MaintenanceExecutor
) : CommandLineRunner {

    companion object {
        private val log = LoggerFactory.getLogger(GitHubActionApplication::class.java)

        @JvmStatic
        fun main(args: Array<String>) {
            runApplication<GitHubActionApplication>(*args)
        }
    }

    override fun run(vararg args: String?) {
        initGitHubApiRateLimiter()
        Environment.validate()

        val parts = Environment.repository.split("/", limit = 2)
        val (owner, repo) = parts

        maintenanceExecutor.deleteOldActionRuns(owner, repo, Environment.retentionDays)
    }

    private fun initGitHubApiRateLimiter() {
        val rateLimit = githubApi.rateLimit()

        // GitHub Actions has a limit of 1000 API requests per hour per repository
        // Source: https://docs.github.com/en/actions/administering-github-actions/usage-limits-billing-and-administration
        val defaultLimit = 1000L
        val defaultRemaining = 1000L
        val defaultReset = Instant.now().plusSeconds(3600).epochSecond

        val rate = rateLimit.rate
        val capacity = rate?.limit ?: defaultLimit
        val refillTokens = rate?.limit ?: defaultLimit
        val refillPeriod = Duration.ofHours(1)
        val refillAlignment = Instant.ofEpochSecond(rate?.reset ?: defaultReset)

        val bandwidth = Bandwidth.builder()
            .capacity(capacity)
            .refillIntervallyAligned(refillTokens, refillPeriod, refillAlignment)
            .build()

        Environment.rateLimiter = Bucket.builder()
            .addLimit(bandwidth)
            .build()

        log.info("Initialized GitHub API rate limiter: limit=${capacity}, remaining=${rate?.remaining ?: defaultRemaining}, resetAt=${refillAlignment}")

        val remaining = rate?.remaining ?: defaultRemaining
        val tokensToCleanup = capacity - remaining
        if (tokensToCleanup > 0) {
            log.info("Consuming $tokensToCleanup tokens to align with current GitHub rate limit state.")
            Environment.rateLimiter.consumeIgnoringRateLimits(tokensToCleanup)
        }
    }
}
