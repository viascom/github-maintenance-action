package io.viascom.github.action.maintenance.api

/** A GitHub answer outside 2xx, carrying the request, the [statusCode] and the start of GitHub's message. */
class GitHubApiException(method: String, url: String, val statusCode: Int, body: String) : RuntimeException(buildMessage(method, url, statusCode, body)) {

    private companion object {
        fun buildMessage(method: String, url: String, statusCode: Int, body: String): String {
            val hint = if (statusCode == 403 && method == "DELETE") " (the token needs the 'actions: write' permission)" else ""
            return "GitHub API $method $url failed with HTTP $statusCode$hint: ${body.take(500)}"
        }
    }
}
