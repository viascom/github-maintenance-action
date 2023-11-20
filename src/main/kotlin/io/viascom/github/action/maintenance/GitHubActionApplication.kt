package io.viascom.github.action.maintenance

import io.viascom.github.action.maintenance.core.Environment
import io.viascom.github.action.maintenance.core.MaintenanceExecutor
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
open class GitHubActionApplication(
    private val maintenanceExecutor: MaintenanceExecutor
) : CommandLineRunner {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            runApplication<GitHubActionApplication>(*args)
        }
    }

    override fun run(vararg args: String?) {
        val (owner, repo) = Environment.repository.split("/", limit = 2)
        maintenanceExecutor.deleteOldActionRuns(owner, repo, Environment.retentionDays)
    }
}