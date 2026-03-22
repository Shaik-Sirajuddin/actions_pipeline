def call(Map config = [:]) {
  // Defaults any repo can override
  def cfg = [
    actionsDir      : config.actionsDir       ?: '.jenkins_actions',
    vaultUrl        : config.vaultUrl         ?: 'http://vault:8200',
    vaultCredential : config.vaultCredential  ?: 'vault-approle',
    vaultSecretPath : config.vaultSecretPath  ?: 'secret/jenkins/github',
    pushgatewayUrl  : config.pushgatewayUrl   ?: 'http://prometheus-pushgateway:9091',
    goVersion       : config.goVersion        ?: '1.22'
  ]

  pipeline {
    agent any
    tools { go "${cfg.goVersion}" }

    environment {
      REPO         = "${env.CHANGE_REPOSITORY ?: 'org/repo'}"
      PR_ID        = "${env.CHANGE_ID}"
      PR_AUTHOR    = "${env.CHANGE_AUTHOR}"
      JOB_ID       = "${env.JOB_NAME}-${env.BUILD_NUMBER}"
      ACTIONS_DIR  = "${cfg.actionsDir}"
      METRICS_FILE = 'jenkins_pr_metrics.prom'
    }

    triggers {
      githubPullRequests()
    }

    stages {

      stage('Fetch Secrets') {
        steps {
          script {
            new org.jenkins.VaultHelper(this).fetchGithubToken(
              cfg.vaultUrl,
              cfg.vaultCredential,
              cfg.vaultSecretPath
            )
          }
        }
      }

      stage('Discover Actions') {
        steps {
          script {
            new org.jenkins.ActionRunner(this).discover(cfg.actionsDir)
          }
        }
      }

      stage('Run Actions') {
        steps {
          script {
            new org.jenkins.ActionRunner(this).runAll()
          }
        }
      }

      stage('Publish PR Comment') {
        steps {
          script {
            new org.jenkins.GithubNotifier(this).publishComment()
          }
        }
      }

      stage('Publish Prometheus Metrics') {
        steps {
          script {
            new org.jenkins.MetricsPublisher(this).publish(cfg.pushgatewayUrl)
          }
        }
      }
    }

    post {
      success {
        script {
          new org.jenkins.GithubNotifier(this).setCommitStatus('jenkins/ci', 'success', 'All actions passed')
        }
      }
      failure {
        script {
          new org.jenkins.GithubNotifier(this).setCommitStatus('jenkins/ci', 'failure', 'Some actions failed')
          new org.jenkins.GithubNotifier(this).postComment("⚠️ **Pipeline error** — check [build logs](${env.BUILD_URL}console)")
        }
      }
      always {
        archiveArtifacts artifacts: 'jenkins_pr_metrics.prom', allowEmptyArchive: true
      }
    }
  }
}