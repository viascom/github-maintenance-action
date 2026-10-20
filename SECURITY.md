# Security

Only the newest release is supported. Report a vulnerability privately through
[GitHub's security advisories](https://github.com/viascom/github-maintenance-action/security/advisories/new),
not in a public issue.

The action receives a token with write access to workflow runs. It sends that token only to the
configured GitHub API base URL, never logs it, and makes no other network connections.
