package io.viascom.github.action.maintenance.core

import io.viascom.github.action.maintenance.api.WorkflowRunFilter
import io.viascom.github.action.maintenance.model.WorkflowRunStatus

/**
 * The action's inputs, read from the `INPUT_*` variables the Actions runner sets for a
 * Docker container action, plus the id of the workflow run executing the action.
 */
data class Settings(
    val gitHubToken: String,
    val gitHubBaseUrl: String,
    val owner: String,
    val repo: String,
    val retentionDays: Int,
    val keepMinimumRuns: Int,
    val deleteLogs: Boolean,
    val deleteArtifacts: Boolean,
    val actors: List<String>,
    val branches: List<String>,
    val events: List<String>,
    val statuses: List<WorkflowRunStatus>,
    val keepPullRequests: Boolean,
    val dryRun: Boolean,
    val debug: Boolean,
    val currentRunId: Long?,
) {
    val deletesEntireRuns: Boolean
        get() = !deleteLogs && !deleteArtifacts

    /**
     * GitHub accepts one actor, branch, event and status per request, so a multi-valued
     * input expands into one listing per combination.
     */
    fun workflowRunFilters(): List<WorkflowRunFilter> = actors.orSingleNull().flatMap { actor ->
        branches.orSingleNull().flatMap { branch ->
            events.orSingleNull().flatMap { event ->
                statuses.orSingleNull().map { status ->
                    WorkflowRunFilter(actor = actor, branch = branch, event = event, status = status)
                }
            }
        }
    }

    private fun <T> List<T>.orSingleNull(): List<T?> = ifEmpty { listOf(null) }

    companion object {
        const val DEFAULT_BASE_URL = "https://api.github.com"
        const val DEFAULT_RETENTION_DAYS = 31
        const val DEFAULT_KEEP_MINIMUM_RUNS = 5

        /**
         * @param env the process environment, usually `System.getenv()`
         * @throws IllegalArgumentException for a missing required input or a value GitHub would not accept
         */
        fun fromEnvironment(env: Map<String, String>): Settings {
            val inputs = ActionInputs(env)
            val repository = inputs.required("repository")
            val parts = repository.split("/", limit = 2)
            require(parts.size == 2 && parts.none(String::isBlank)) {
                "input 'repository' must be in the format 'owner/repo', got '$repository'"
            }
            return Settings(
                gitHubToken = inputs.required("github_token"),
                gitHubBaseUrl = (inputs.optional("github_base_url") ?: DEFAULT_BASE_URL).trimEnd('/'),
                owner = parts[0],
                repo = parts[1],
                retentionDays = inputs.nonNegativeInt("retention_days", DEFAULT_RETENTION_DAYS),
                keepMinimumRuns = inputs.nonNegativeInt("keep_minimum_runs", DEFAULT_KEEP_MINIMUM_RUNS),
                deleteLogs = inputs.boolean("delete_logs"),
                deleteArtifacts = inputs.boolean("delete_artifacts"),
                actors = inputs.list("actors"),
                branches = inputs.list("branches"),
                events = inputs.list("events").map(String::lowercase),
                statuses = inputs.list("statuses").map(WorkflowRunStatus::parse),
                keepPullRequests = inputs.boolean("keep_pull_requests"),
                dryRun = inputs.boolean("dry_run"),
                debug = inputs.boolean("debug"),
                currentRunId = env["GITHUB_RUN_ID"]?.toLongOrNull(),
            )
        }
    }
}

private class ActionInputs(private val env: Map<String, String>) {

    fun optional(name: String): String? = env["INPUT_${name.uppercase()}"]?.trim()?.takeIf(String::isNotEmpty)

    fun required(name: String): String = optional(name) ?: throw IllegalArgumentException("input '$name' is required")

    fun nonNegativeInt(name: String, default: Int): Int {
        val raw = optional(name) ?: return default
        return raw.toIntOrNull()?.takeIf { it >= 0 }
            ?: throw IllegalArgumentException("input '$name' must be a whole number >= 0, got '$raw'")
    }

    fun boolean(name: String): Boolean = when (val raw = optional(name)?.lowercase()) {
        null, "false" -> false
        "true" -> true
        else -> throw IllegalArgumentException("input '$name' must be 'true' or 'false', got '$raw'")
    }

    fun list(name: String): List<String> = optional(name)?.split(",")?.map(String::trim)?.filter(String::isNotEmpty) ?: emptyList()
}
