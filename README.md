# GitHub Maintenance Action

GitHub Maintenance Action is a GitHub Action designed for automated housekeeping of your GitHub repositories. It offers
configurable maintenance tasks such as deleting workflow runs, logs, and artifacts. The action is highly flexible,
allowing users to specify what to clean and when aligning with individual project needs.

## Key Features

* __Customizable Workflow Run Cleanup:__ Automatically delete old or irrelevant workflow runs based on user-defined criteria like age, status, or branch.
* __Log Management:__ Clean up workflow logs to keep your repository efficient and clutter-free.
* __Artifact Control:__ Configure rules to delete artifacts, helping manage storage space and ensuring only relevant artifacts are retained.
* __User-Friendly Configuration:__ Set up the action quickly with intuitive parameters, making defining what to clean and when easy.

## Usage

To use GitHub Maintenance Action, add it to your .github/workflows directory in your repository. You can customize the
action's YAML configuration file settings to suit your project's maintenance needs.

```yaml
name: GitHub Maintenance
on:
  workflow_dispatch: # To be able to run manually.
  schedule:
    - cron: '0 3 * * *' # Run daily at 03:00.

jobs:
  github_maintenance:
    name: Execute Maintenance
    runs-on: ubuntu-latest
    
    permissions:
      actions: write
      
    steps:
      - name: Delete workflow runs
        uses: viascom/github-maintenance-action@v0.1.0
```

## Configuration

| Input Name               | Description                                                          | Default Value              |
|--------------------------|----------------------------------------------------------------------|----------------------------|
| `github_token`           | Authentication token                                                 | `${{ github.token }}`      |
| `github_base_url`        | Base API URL                                                         | `https://api.github.com`   |
| `repository`             | Name of the repository.                                              | `${{ github.repository }}` |
| `retention_days`         | Retention time in days of runs to keep.                              | 31                         |
| `keep_minimum_runs`      | Minimum workflow runs to keep.                                       | 5                          |
| `delete_logs`            | Deletes only the logs of the workflow runs.                          | false                      |
| `delete_artifacts`       | Deletes only the artifacts of the workflow runs.                     | false                      |
| `actors`                 | Comma-separated list of actors of the workflow runs to be deleted.   | null                       |
| `branches`               | Comma-separated list of branches of the workflow runs to be deleted. | null                       |
| `events`                 | Comma-separated list of events of the workflow runs to be deleted.   | null                       |
| `statuses`               | Comma-separated list of statuses of the workflow runs to be deleted. | null                       |
| `keep_pull_request_runs` | If set to true, it will keep pull request workflow runs.             | false                      |
| `dry_run`                | Logs simulated changes, no actions are performed!                    | false                      |
| `debug`                  | When debug is enabled more logs will be printed.                     | false                      |

## Example with all configurations

```yaml
name: GitHub Maintenance
on:
  workflow_dispatch: # To be able to run manually.
  schedule:
    - cron: '0 3 * * *' # Run daily at 03:00.

jobs:
  github_maintenance:
    name: Execute Maintenance
    runs-on: ubuntu-latest

    permissions:
      actions: write

    steps:
      - name: Delete workflow runs
        uses: viascom/github-maintenance-action@v0.0.1
        with:
          github_token: ${{ secrets.GITHUB_TOKEN }}
          github_base_url: 'https://api.github.com'
          repository: ${{ github.repository }}
          retention_days: 31
          keep_minimum_runs: 5
          delete_logs: false
          delete_artifacts: false
          actors: 'nik-sta, itsmefox'
          branches: 'main, develop'
          events: 'push, workflow_dispatch'
          statuses: 'failure'
          keep_pull_requests: false
          dry_run: false
          debug: false
```
