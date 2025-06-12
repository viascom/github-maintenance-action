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

        val capacity = rateLimit.rate.limit
        val refillTokens = rateLimit.rate.limit
        val refillPeriod = Duration.ofHours(1)
        val refillAlignment = Instant.ofEpochSecond(rateLimit.rate.reset)

        val bandwidth = Bandwidth.builder()
            .capacity(capacity)
            .refillIntervallyAligned(refillTokens, refillPeriod, refillAlignment)
            .build()

        Environment.rateLimiter = Bucket.builder()
            .addLimit(bandwidth)
            .build()

        log.info("Initialized GitHub API rate limiter: limit=${capacity}, remaining=${rateLimit.rate.remaining}, resetAt=${refillAlignment}")

        val tokensToCleanup = capacity - rateLimit.rate.remaining
        if (tokensToCleanup > 0) {
            log.info("Consuming $tokensToCleanup tokens to align with current GitHub rate limit state.")
            Environment.rateLimiter.consumeIgnoringRateLimits(tokensToCleanup)
        }
    }
}
