package io.viascom.github.action.maintenance

import io.viascom.github.action.maintenance.api.GitHubApi
import io.viascom.github.action.maintenance.core.ActionsReporter
import io.viascom.github.action.maintenance.core.MaintenanceExecutor
import io.viascom.github.action.maintenance.core.Settings
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean

@SpringBootApplication
class GitHubActionApplication {

    @Bean
    fun settings(): Settings = Settings.fromEnvironment(System.getenv())

    @Bean
    fun gitHubApi(settings: Settings): GitHubApi = GitHubApi(settings.gitHubBaseUrl, settings.gitHubToken, settings.owner, settings.repo)

    @Bean
    fun maintenanceExecutor(gitHubApi: GitHubApi, settings: Settings): MaintenanceExecutor = MaintenanceExecutor(gitHubApi, settings)

    @Bean
    fun maintenance(executor: MaintenanceExecutor): CommandLineRunner = CommandLineRunner {
        val summary = executor.run()
        ActionsReporter(System.getenv()).publish(summary)
        summary.requireNoFailures()
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            if (System.getenv("INPUT_DEBUG").equals("true", ignoreCase = true)) {
                System.setProperty("logging.level.io.viascom.github.action.maintenance", "DEBUG")
            }
            runApplication<GitHubActionApplication>(*args)
        }
    }
}
