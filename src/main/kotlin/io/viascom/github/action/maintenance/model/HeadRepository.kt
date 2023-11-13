package io.viascom.github.action.maintenance.model

import com.google.gson.annotations.SerializedName


data class HeadRepository(

    @SerializedName("id")
    var id: Long? = null,

    @SerializedName("node_id")
    var nodeId: String? = null,

    @SerializedName("name")
    var name: String? = null,

    @SerializedName("full_name")
    var fullName: String? = null,

    @SerializedName("private")
    var private: Boolean? = null,

    @SerializedName("owner")
    var owner: Actor? = Actor(),

    @SerializedName("html_url")
    var htmlUrl: String? = null,

    @SerializedName("description")
    var description: String? = null,

    @SerializedName("fork")
    var fork: Boolean? = null,

    @SerializedName("url")
    var url: String? = null,

    @SerializedName("forks_url")
    var forksUrl: String? = null,

    @SerializedName("keys_url")
    var keysUrl: String? = null,

    @SerializedName("collaborators_url")
    var collaboratorsUrl: String? = null,

    @SerializedName("teams_url")
    var teamsUrl: String? = null,

    @SerializedName("hooks_url")
    var hooksUrl: String? = null,

    @SerializedName("issue_events_url")
    var issueEventsUrl: String? = null,

    @SerializedName("events_url")
    var eventsUrl: String? = null,

    @SerializedName("assignees_url")
    var assigneesUrl: String? = null,

    @SerializedName("branches_url")
    var branchesUrl: String? = null,

    @SerializedName("tags_url")
    var tagsUrl: String? = null,

    @SerializedName("blobs_url")
    var blobsUrl: String? = null,

    @SerializedName("git_tags_url")
    var gitTagsUrl: String? = null,

    @SerializedName("git_refs_url")
    var gitRefsUrl: String? = null,

    @SerializedName("trees_url")
    var treesUrl: String? = null,

    @SerializedName("statuses_url")
    var statusesUrl: String? = null,

    @SerializedName("languages_url")
    var languagesUrl: String? = null,

    @SerializedName("stargazers_url")
    var stargazersUrl: String? = null,

    @SerializedName("contributors_url")
    var contributorsUrl: String? = null,

    @SerializedName("subscribers_url")
    var subscribersUrl: String? = null,

    @SerializedName("subscription_url")
    var subscriptionUrl: String? = null,

    @SerializedName("commits_url")
    var commitsUrl: String? = null,

    @SerializedName("git_commits_url")
    var gitCommitsUrl: String? = null,

    @SerializedName("comments_url")
    var commentsUrl: String? = null,

    @SerializedName("issue_comment_url")
    var issueCommentUrl: String? = null,

    @SerializedName("contents_url")
    var contentsUrl: String? = null,

    @SerializedName("compare_url")
    var compareUrl: String? = null,

    @SerializedName("merges_url")
    var mergesUrl: String? = null,

    @SerializedName("archive_url")
    var archiveUrl: String? = null,

    @SerializedName("downloads_url")
    var downloadsUrl: String? = null,

    @SerializedName("issues_url")
    var issuesUrl: String? = null,

    @SerializedName("pulls_url")
    var pullsUrl: String? = null,

    @SerializedName("milestones_url")
    var milestonesUrl: String? = null,

    @SerializedName("notifications_url")
    var notificationsUrl: String? = null,

    @SerializedName("labels_url")
    var labelsUrl: String? = null,

    @SerializedName("releases_url")
    var releasesUrl: String? = null,

    @SerializedName("deployments_url")
    var deploymentsUrl: String? = null


)