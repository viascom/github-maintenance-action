package io.viascom.github.action.maintenance

import io.viascom.github.action.maintenance.core.Environment
import io.viascom.github.action.maintenance.core.MaintenanceExecutor
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
open class GitHubActionApplication : CommandLineRunner {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            runApplication<GitHubActionApplication>(*args)
        }
    }

    override fun run(vararg args: String?) {
        val (owner, repo) = Environment.repository.split("/", limit = 2)
        MaintenanceExecutor().deleteOldActionRuns(owner, repo, Environment.retentionDays)
    }
}