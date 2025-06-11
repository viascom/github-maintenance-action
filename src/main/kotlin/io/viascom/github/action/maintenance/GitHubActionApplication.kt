package io.viascom.github.action.maintenance

import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Bucket
import io.viascom.github.action.maintenance.api.GitHubApi
import io.viascom.github.action.maintenance.core.Environment
import io.viascom.github.action.maintenance.core.MaintenanceExecutor
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
        @JvmStatic
        fun main(args: Array<String>) {
            runApplication<GitHubActionApplication>(*args)
        }
    }

    override fun run(vararg args: String?) {
        initGitHubApiRateLimiter()

        val (owner, repo) = Environment.repository.split("/", limit = 2)
        maintenanceExecutor.deleteOldActionRuns(owner, repo, Environment.retentionDays)
    }

    private fun initGitHubApiRateLimiter() {
        val rateLimit = githubApi.rateLimit()
        Environment.rateLimiter =
            Bucket.builder()
                .addLimit(
                    Bandwidth.builder().capacity(rateLimit.rate.limit)
                        .refillIntervallyAligned(rateLimit.rate.limit, Duration.ofHours(1), Instant.ofEpochSecond(rateLimit.rate.reset)).build()
                ).build()
        val tokensToCleanup = rateLimit.rate.limit - rateLimit.rate.remaining
        if (tokensToCleanup > 0) {
            Environment.rateLimiter.consumeIgnoringRateLimits(rateLimit.rate.limit - rateLimit.rate.remaining)
        }
    }
}
