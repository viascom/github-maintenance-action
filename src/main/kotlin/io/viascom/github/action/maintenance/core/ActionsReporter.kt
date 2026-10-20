package io.viascom.github.action.maintenance.core

import java.io.File

/**
 * Publishes the result where a workflow can read it: as step outputs via `GITHUB_OUTPUT`
 * and as a Markdown table in the job summary via `GITHUB_STEP_SUMMARY`. Outside of a
 * workflow neither file exists and nothing is written.
 */
class ActionsReporter(private val env: Map<String, String>) {

    /** Appends the step outputs and the job summary for [summary]. */
    fun publish(summary: MaintenanceSummary) {
        env["GITHUB_OUTPUT"]?.let { path ->
            File(path).appendText(
                """
                runs_deleted=${summary.runsDeleted}
                logs_deleted=${summary.logsDeleted}
                artifacts_deleted=${summary.artifactsDeleted}
                failures=${summary.failures}

                """.trimIndent(),
            )
        }
        env["GITHUB_STEP_SUMMARY"]?.let { path ->
            val title = if (summary.dryRun) "GitHub Maintenance (dry run)" else "GitHub Maintenance"
            File(path).appendText(
                """
                ### $title

                | Result | Count |
                |---|---:|
                | Workflow runs ${summary.verb} | ${summary.runsDeleted} |
                | Logs ${summary.verb} | ${summary.logsDeleted} |
                | Artifacts ${summary.verb} | ${summary.artifactsDeleted} |
                | Failures | ${summary.failures} |

                """.trimIndent(),
            )
        }
    }
}
