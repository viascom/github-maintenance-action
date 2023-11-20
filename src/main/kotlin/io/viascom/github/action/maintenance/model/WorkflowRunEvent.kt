package io.viascom.github.action.maintenance.model

enum class WorkflowRunEvent(val value: String) {
    BRANCH_PROTECTION_RULE("branch_protection_rule"),
    CHECK_RUN("check_run"),
    CHECK_SUITE("check_suite"),
    CREATE("create"),
    DELETE("delete"),
    DEPLOYMENT("deployment"),
    DEPLOYMENT_STATUS("deployment_status"),
    DISCUSSION("discussion"),
    DISCUSSION_COMMENT("discussion_comment"),
    FORK("fork"),
    GOLLUM("gollum"),
    ISSUE_COMMENT("issue_comment"),
    ISSUES("issues"),
    LABEL("label"),
    MERGE_GROUP("merge_group"),
    MILESTONE("milestone"),
    PAGE_BUILD("page_build"),
    PROJECT("project"),
    PROJECT_CARD("project_card"),
    PROJECT_COLUMN("project_column"),
    PUBLIC("public"),
    PULL_REQUEST("pull_request"),
    PULL_REQUEST_COMMENT("issue_comment"), // Note: this uses "issue_comment"
    PULL_REQUEST_REVIEW("pull_request_review"),
    PULL_REQUEST_REVIEW_COMMENT("pull_request_review_comment"),
    PULL_REQUEST_TARGET("pull_request_target"),
    PUSH("push"),
    REGISTRY_PACKAGE("registry_package"),
    RELEASE("release"),
    REPOSITORY_DISPATCH("repository_dispatch"),
    SCHEDULE("schedule"),
    STATUS("status"),
    WATCH("watch"),
    WORKFLOW_CALL("workflow_call"),
    WORKFLOW_DISPATCH("workflow_dispatch"),
    WORKFLOW_RUN("workflow_run");

companion object {
    private val map = entries.associateBy(WorkflowRunEvent::value)
    fun fromValue(value: String) = map[value]

    fun fromCommaSeparatedValues(values: String): List<WorkflowRunEvent> {
        if (values.isBlank()) {
            return arrayListOf()
        }

        return values.split(",")
            .map { it.trim() }
            .mapNotNull { fromValue(it) }
    }

    fun toCommaSeparatedString(statuses: List<WorkflowRunEvent>): String {
        return statuses.joinToString(separator = ", ") { it.value }
    }
}
}
