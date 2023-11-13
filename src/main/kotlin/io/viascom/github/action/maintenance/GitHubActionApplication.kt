package io.viascom.github.action.maintenance

import io.viascom.github.action.maintenance.api.GitHubApi
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@SpringBootApplication
open class GitHubActionApplication : CommandLineRunner {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            runApplication<GitHubActionApplication>(*args)
        }
    }

    private val log = LoggerFactory.getLogger(javaClass)
    private val dryRun = System.getenv("DRY_RUN").toBoolean()

    override fun run(vararg args: String?) {
        val githubRepository = System.getenv("GITHUB_REPOSITORY") ?: ""
        val (owner, repo) = githubRepository.split("/", limit = 2)

        val retentionDays = System.getenv("RETENTION_DAYS").toLong()

        deleteOldActionRuns(owner, repo, retentionDays)
    }

    private fun deleteOldActionRuns(owner: String, repo: String, retentionDays: Long) {
        val githubApi = GitHubApi()

        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val formattedDate = LocalDateTime.now().minusDays(retentionDays).format(formatter)

        val workflowRuns = githubApi.listWorkflowRuns(
            owner = owner,
            repo = repo,
            created = "*..$formattedDate",
        )

        log.info("\uD83D\uDD0D Found ${workflowRuns.runs.size} workflow runs to clean up ...")
        workflowRuns.runs.forEach { run ->
            log.info("\uD83E\uDEE7 Deleted workflow run with id ${run.id}.")
            if (!dryRun) { githubApi.deleteWorkflowRun(owner, repo, run.id) }
        }

    }
}