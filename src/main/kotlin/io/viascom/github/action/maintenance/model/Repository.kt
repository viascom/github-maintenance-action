package io.viascom.github.action.maintenance.model

import com.google.gson.annotations.SerializedName

data class Repository(
    @SerializedName("id")
    var id: Long? = null,

    @SerializedName("node_id")
    var nodeId: String? = null,

    @SerializedName("name")
    var name: String? = null,

    @SerializedName("full_name")
    var fullName: String? = null,

    @SerializedName("owner")
    var owner: Actor? = Actor(),

    @SerializedName("private")
    var private: Boolean? = null,

    @SerializedName("html_url")
    var htmlUrl: String? = null,

    @SerializedName("description")
    var description: String? = null,

    @SerializedName("fork")
    var fork: Boolean? = null,

    @SerializedName("url")
    var url: String? = null,

    @SerializedName("archive_url")
    var archiveUrl: String? = null,

    @SerializedName("assignees_url")
    var assigneesUrl: String? = null,

    @SerializedName("blobs_url")
    var blobsUrl: String? = null,

    @SerializedName("branches_url")
    var branchesUrl: String? = null,

    @SerializedName("collaborators_url")
    var collaboratorsUrl: String? = null,

    @SerializedName("comments_url")
    var commentsUrl: String? = null,

    @SerializedName("commits_url")
    var commitsUrl: String? = null,

    @SerializedName("compare_url")
    var compareUrl: String? = null,

    @SerializedName("contents_url")
    var contentsUrl: String? = null,

    @SerializedName("contributors_url")
    var contributorsUrl: String? = null,

    @SerializedName("deployments_url")
    var deploymentsUrl: String? = null,

    @SerializedName("downloads_url")
    var downloadsUrl: String? = null,

    @SerializedName("events_url")
    var eventsUrl: String? = null,

    @SerializedName("forks_url")
    var forksUrl: String? = null,

    @SerializedName("git_commits_url")
    var gitCommitsUrl: String? = null,

    @SerializedName("git_refs_url")
    var gitRefsUrl: String? = null,

    @SerializedName("git_tags_url")
    var gitTagsUrl: String? = null,

    @SerializedName("git_url")
    var gitUrl: String? = null,

    @SerializedName("issue_comment_url")
    var issueCommentUrl: String? = null,

    @SerializedName("issue_events_url")
    var issueEventsUrl: String? = null,

    @SerializedName("issues_url")
    var issuesUrl: String? = null,

    @SerializedName("keys_url")
    var keysUrl: String? = null,

    @SerializedName("labels_url")
    var labelsUrl: String? = null,

    @SerializedName("languages_url")
    var languagesUrl: String? = null,

    @SerializedName("merges_url")
    var mergesUrl: String? = null,

    @SerializedName("milestones_url")
    var milestonesUrl: String? = null,

    @SerializedName("notifications_url")
    var notificationsUrl: String? = null,

    @SerializedName("pulls_url")
    var pullsUrl: String? = null,

    @SerializedName("releases_url")
    var releasesUrl: String? = null,

    @SerializedName("ssh_url")
    var sshUrl: String? = null,

    @SerializedName("stargazers_url")
    var stargazersUrl: String? = null,

    @SerializedName("statuses_url")
    var statusesUrl: String? = null,

    @SerializedName("subscribers_url")
    var subscribersUrl: String? = null,

    @SerializedName("subscription_url")
    var subscriptionUrl: String? = null,

    @SerializedName("tags_url")
    var tagsUrl: String? = null,

    @SerializedName("teams_url")
    var teamsUrl: String? = null,

    @SerializedName("trees_url")
    var treesUrl: String? = null,

    @SerializedName("hooks_url")
    var hooksUrl: String? = null
)
