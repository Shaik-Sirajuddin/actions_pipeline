# Actions Jenkins Shared Library

A standard Jenkins Shared Library for executing pull request pipelines with integrated secret management, GitHub notifications, and monitoring.

## 🚀 Overview

This library provides a simplified way to run CI tasks (actions) on GitHub pull requests. It automatically handles:
- **Secret Fetching**: Retrieves GitHub tokens or other repository secrets from HashiCorp Vault.
- **Action Discovery**: Scans for executable scripts in a specified directory (e.g., `.jenkins_actions/`).
- **Parallel/Sequential Execution**: Runs all discovered actions and tracks their status.
- **GitHub Notifications**: Posts summary comments on PRs and updates commit statuses.
- **Prometheus Metrics**: Pushes execution results and durations to Prometheus Pushgateway.

## 📂 File Structure

```text
.
├── vars/
│   └── prPipeline.groovy       # Main pipeline entry point
├── src/org/jenkins/
│   ├── ActionRunner.groovy      # Script discovery and execution logic
│   ├── GithubNotifier.groovy    # GitHub API interactions (comments & status)
│   ├── VaultHelper.groovy       # Vault secret retrieval helper
│   └── MetricsPublisher.groovy  # Prometheus metrics formatting and pushing
└── resources/
    └── pipeline-config.yaml    # Default configuration parameters
```

## 🛠 Usage

1. **Configure the Shared Library** in your Jenkins global settings.
2. **Create a `Jenkinsfile`** in your repository:

```groovy
@Library('actions-jenkins') _

prPipeline([
    goVersion: '1.22',
    actionsDir: '.jenkins_actions'
])
```

3. **Define Actions**: Place executable scripts in the `actionsDir` (e.g., `test.sh`, `lint.sh`).

## ⚙️ Configuration

The pipeline can be configured via the `Map` passed to `prPipeline()` or defaults in `resources/pipeline-config.yaml`.

| Parameter | Default | Description |
|-----------|---------|-------------|
| `actionsDir` | `.jenkins_actions` | Directory containing CI scripts |
| `vaultUrl` | `http://vault:8200` | URL of the HashiCorp Vault server |
| `vaultCredential` | `vault-approle` | Jenkins credential ID for Vault access |
| `goVersion` | `1.22` | Go tool version to use |

## 📊 Monitoring

Metrics are pushed to Prometheus Pushgateway and can be visualized in Grafana.
Metrics exported:
- `jenkins_pr_action_duration_seconds`
- `jenkins_pr_action_status` (1 = pass, 0 = fail)
- `jenkins_pr_total_actions`
- `jenkins_pr_passed_actions`
- `jenkins_pr_failed_actions`
