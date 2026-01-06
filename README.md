# GitHub Maintenance Action

[![GitHub release](https://img.shields.io/github/v/release/viascom/github-maintenance-action)](https://github.com/viascom/github-maintenance-action/releases)  
[![GitHub license](https://img.shields.io/github/license/viascom/github-maintenance-action)](LICENSE)

**GitHub Maintenance Action** is a lightweight GitHub Action that automates housekeeping of your GitHub repositories.  
It helps you keep your repositories clean by regularly deleting old workflow runs, artifacts, and logs — configurable to match your project needs.

## Key Features

- 🗑️ **Customizable Workflow Run Cleanup:** Automatically delete old or irrelevant workflow runs based on age, status, actor, event, branch.
- 🗂️ **Log Management:** Clean up workflow logs to manage storage and keep your repository efficient.
- 📦 **Artifact Control:** Configure deletion of artifacts to manage storage space and retain only relevant data.
- ⚙️ **Flexible Configuration:** Simple parameters allow you to define exactly what to clean and when.
- 🛡️ **Dry Run Support:** Simulate deletions first — great for testing your config safely.

## Usage

Add the action to your workflow in `.github/workflows/maintenance.yml`.  
You can configure it to run on a schedule or manually:

```yaml
name: GitHub Maintenance
on:
    workflow_dispatch: # Allow manual triggering
    schedule:
        -   cron: '0 3 * * *' # Run daily at 03:00 UTC

jobs:
    github_maintenance:
        name: Execute Maintenance
        runs-on: ubuntu-latest

        permissions:
            actions: write # Required to delete workflow runs / logs / artifacts

        steps:
            -   name: Delete workflow runs
                uses: viascom/github-maintenance-action@v0.2.0
                with:
                    github_token: ${{ secrets.GITHUB_TOKEN }}
```

## Configuration

| Input Name           | Description                                                          | Default Value              |
|----------------------|----------------------------------------------------------------------|----------------------------|
| `github_token`       | Authentication token                                                 | `${{ github.token }}`      |
| `github_base_url`    | Base API URL                                                         | `https://api.github.com`   |
| `repository`         | Name of the repository.                                              | `${{ github.repository }}` |
| `retention_days`     | Retention time in days of runs to keep.                              | 31                         |
| `keep_minimum_runs`  | Minimum workflow runs to keep.                                       | 5                          |
| `delete_logs`        | Deletes only the logs of the workflow runs.                          | false                      |
| `delete_artifacts`   | Deletes only the artifacts of the workflow runs.                     | false                      |
| `actors`             | Comma-separated list of actors of the workflow runs to be deleted.   | (empty)                    |
| `branches`           | Comma-separated list of branches of the workflow runs to be deleted. | (empty)                    |
| `events`             | Comma-separated list of events of the workflow runs to be deleted.   | (empty)                    |
| `statuses`           | Comma-separated list of statuses of the workflow runs to be deleted. | (empty)                    |
| `keep_pull_requests` | If set to true, it will keep pull request workflow runs.             | false                      |
| `dry_run`            | Logs simulated changes, no actions are performed!                    | false                      |
| `debug`              | When debug is enabled more logs will be printed.                     | false                      |

## Example with all configurations

```yaml
name: GitHub Maintenance
on:
    workflow_dispatch: # Allow manual triggering
    schedule:
        -   cron: '0 3 * * *' # Run daily at 03:00 UTC
        
jobs:
    github_maintenance:
        name: Execute Maintenance
        runs-on: ubuntu-latest
        
        permissions:
            actions: write
        
        steps:
            -   name: Delete workflow runs
                uses: viascom/github-maintenance-action@v0.2.0
                with:
                    github_token: ${{ secrets.GITHUB_TOKEN }}
                    github_base_url: 'https://api.github.com'
                    repository: ${{ github.repository }}
                    retention_days: 31
                    keep_minimum_runs: 5
                    delete_logs: false
                    delete_artifacts: false
                    actors: 'nik-sta,itsmefox'
                    branches: 'main,develop'
                    events: 'push,workflow_dispatch'
                    statuses: 'failure'
                    keep_pull_requests: false
                    dry_run: false
                    debug: false
```

## Notes

* If both delete_logs and delete_artifacts are false, the entire workflow run will be deleted.
* If dry_run is enabled → no deletions will occur, but you will see logs indicating what would have been deleted.

## License

MIT License © Viascom Ltd liab. Co
