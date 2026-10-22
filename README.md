# GitHub Maintenance Action

[![Release](https://img.shields.io/github/v/release/viascom/github-maintenance-action)](https://github.com/viascom/github-maintenance-action/releases)
[![CI](https://github.com/viascom/github-maintenance-action/actions/workflows/ci.yml/badge.svg)](https://github.com/viascom/github-maintenance-action/actions/workflows/ci.yml)
[![License](https://img.shields.io/github/license/viascom/github-maintenance-action)](LICENSE)

Deletes old workflow runs from a repository, or only their logs and artifacts. You choose the
retention in days, how many of the newest runs always stay, and optional filters on actor,
branch, event and status. A dry run lists what would go without deleting anything.

## Usage

```yaml
name: GitHub Maintenance
on:
  workflow_dispatch:
  schedule:
    - cron: "0 3 * * *"

jobs:
  maintenance:
    runs-on: ubuntu-latest
    permissions:
      actions: write
    steps:
      - uses: viascom/github-maintenance-action@v1
        with:
          retention_days: 31
          keep_minimum_runs: 5
```

Run it once with `dry_run: true` first. The log and the job summary show every run it would
touch.

## What it does

1. Lists the newest `keep_minimum_runs` runs of the repository. Those are never touched.
2. Lists the runs created on or before today minus `retention_days` (UTC), one GitHub query
   per combination of the `actors`, `branches`, `events` and `statuses` values. GitHub returns
   at most 1000 runs per query, so a repository with more old runs than that is cleaned over
   several scheduled runs.
3. Drops the kept runs, the run that is executing the action, queued and running runs unless
   `statuses` names them, and, with `keep_pull_requests`, every run triggered by `pull_request`
   or `pull_request_target`.
4. For each remaining run: deletes its logs if `delete_logs` is set, its artifacts if
   `delete_artifacts` is set, and the whole run if neither is set. It waits one second between
   deletions, as GitHub asks for mutating requests.
5. Writes the counts to the step outputs and the job summary. If any single deletion failed,
   the step fails after all runs were processed, so a missing permission cannot pass silently.

## Inputs

| Input                | Default                  | Description                                                                                                   |
|----------------------|--------------------------|---------------------------------------------------------------------------------------------------------------|
| `github_token`       | `${{ github.token }}`    | Token with `actions: write` on the repository.                                                                |
| `repository`         | `${{ github.repository }}` | Repository to clean, as `owner/repo`.                                                                       |
| `retention_days`     | `31`                     | Runs created more than this many days ago are candidates. `0` means every run.                                |
| `keep_minimum_runs`  | `5`                      | The newest runs that always stay, whatever their age.                                                         |
| `github_base_url`    | `https://api.github.com` | REST API base URL. For GitHub Enterprise Server: `https://HOST/api/v3`.                                       |
| `delete_logs`        | `false`                  | Delete only the logs of matching runs.                                                                        |
| `delete_artifacts`   | `false`                  | Delete only the artifacts of matching runs.                                                                   |
| `actors`             |                          | Comma-separated logins. Only runs triggered by these users are candidates.                                    |
| `branches`           |                          | Comma-separated branch names.                                                                                 |
| `events`             |                          | Comma-separated trigger events, for example `push,schedule`.                                                  |
| `statuses`           |                          | Comma-separated statuses or conclusions: `completed`, `success`, `failure`, `cancelled`, `skipped`, `timed_out`, `action_required`, `neutral`, `stale`, `in_progress`, `queued`, `requested`, `waiting`, `pending`. |
| `keep_pull_requests` | `false`                  | Keep runs triggered by `pull_request` and `pull_request_target`.                                              |
| `dry_run`            | `false`                  | Log what would be deleted and delete nothing.                                                                 |
| `debug`              | `false`                  | Log every API request.                                                                                        |

Filters are optional. An empty filter means no restriction on that dimension. With
`delete_logs` and `delete_artifacts` both set, the run itself stays and loses its logs and
artifacts. An input with a value GitHub does not accept, such as `statuses: failed`, fails the
step before anything is listed.

## Outputs

| Output              | Description                                                        |
|---------------------|--------------------------------------------------------------------|
| `runs_deleted`      | Workflow runs deleted, or that would be deleted in a dry run.      |
| `logs_deleted`      | Run logs deleted, or that would be deleted in a dry run.           |
| `artifacts_deleted` | Artifacts deleted, or that would be deleted in a dry run.          |
| `failures`          | Operations that failed. The step fails when this is not `0`.       |

## Examples

Keep the runs but reclaim storage by removing logs and artifacts older than a week:

```yaml
- uses: viascom/github-maintenance-action@v1
  with:
    retention_days: 7
    keep_minimum_runs: 20
    delete_logs: true
    delete_artifacts: true
```

Remove failed and cancelled runs of scheduled workflows on `main` after three days, keep
pull request runs:

```yaml
- uses: viascom/github-maintenance-action@v1
  with:
    retention_days: 3
    branches: main
    events: schedule
    statuses: failure,cancelled
    keep_pull_requests: true
```

Clean another repository with a fine-grained personal access token that has
`Actions: Read and write` on it:

```yaml
- uses: viascom/github-maintenance-action@v1
  with:
    github_token: ${{ secrets.MAINTENANCE_TOKEN }}
    repository: viascom/other-repository
```

## Permissions and rate limits

The job needs `permissions: actions: write`. Listing runs and artifacts needs `actions: read`,
which `write` includes. Deleting a run also removes its logs and artifacts.

The `GITHUB_TOKEN` is limited to 1000 API requests per hour per repository, and GitHub applies
secondary limits to bursts of deletions. The action honours the `retry-after` and
`x-ratelimit-reset` headers: when a request is rate limited it waits as instructed and retries
up to three times before counting the operation as failed.

## Versioning

Releases follow semantic versioning. `@v1` follows the newest 1.x release, `@v1.0.0` pins one.
Every release ships a Docker image under the same version to `ghcr.io/viascom/github-maintenance-action`
and `docker.io/viascom/github-maintenance-action`, and `action.yml` at a release tag references
that image by digest, not by tag. A pinned action therefore runs the same image bytes on every
run, whatever happens to the tags in the registry.

## Development

Kotlin 2.4 on Java 25 (Zulu), Spring Boot 4.1, OkHttp 5 and Gson. Tests use JUnit 5, MockK,
atrium and MockWebServer; `./gradlew check` runs them with the JaCoCo coverage gate.

```bash
./gradlew build
docker build -f docker/Dockerfile -t github-maintenance-action .
docker run --rm \
  -e INPUT_GITHUB_TOKEN="$(gh auth token)" \
  -e INPUT_REPOSITORY=viascom/github-maintenance-action \
  -e INPUT_DRY_RUN=true \
  github-maintenance-action
```

The Actions runner hands inputs to a Docker container action as `INPUT_<NAME>` variables, so
running the image directly behaves exactly like the action.

To release, set the version in `build.gradle`, merge, and push the tag `vX.Y.Z`. The release
workflow verifies the version, runs the build, pushes the multi-arch image, pins `action.yml` to
the digest of that image in a commit it pushes to `main`, moves the tags `vX.Y.Z` and `vX` onto
that commit, and publishes the GitHub release notes. When `main` moved on since the tag, the
tags stay on the release commit and `main` gets its own pin commit, as long as `main` still
declares the same version. GitHub creates a new
container package as private, so after the first release set the package
`ghcr.io/viascom/github-maintenance-action` to public, or no workflow can pull the image.

## License

[MIT](LICENSE), Viascom Ltd liab. Co.
