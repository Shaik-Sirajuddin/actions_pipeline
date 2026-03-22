def call(Map config = [:]) {
  def cfg = [
    actionsDir      : config.actionsDir       ?: '.jenkins_actions',
    vaultUrl        : config.vaultUrl         ?: 'http://vault.vault.svc.cluster.local:8200',
    vaultCredential : config.vaultCredential  ?: 'jenkins-vault-approle',
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
            def log = new org.jenkins.LogHelper(this)
            log.time('VaultHelper.fetchGithubToken') {
              new org.jenkins.VaultHelper(this).fetchGithubToken(
                cfg.vaultUrl,
                cfg.vaultCredential,
                cfg.vaultSecretPath
              )
            }
          }
        }
      }

      stage('Discover Actions') {
        steps {
          script {
            def log = new org.jenkins.LogHelper(this)
            log.time('ActionRunner.discover') {
              new org.jenkins.ActionRunner(this).discover(cfg.actionsDir)
            }
          }
        }
      }

      stage('Run Actions') {
        steps {
          script {
            def log = new org.jenkins.LogHelper(this)
            log.time('ActionRunner.runAll') {
              new org.jenkins.ActionRunner(this).runAll()
            }
          }
        }
      }

      stage('Publish PR Comment') {
        steps {
          script {
            def log = new org.jenkins.LogHelper(this)
            log.time('GithubNotifier.publishComment') {
              new org.jenkins.GithubNotifier(this).publishComment()
            }
          }
        }
      }

      stage('Publish Prometheus Metrics') {
        steps {
          script {
            def log = new org.jenkins.LogHelper(this)
            log.time('MetricsPublisher.publish') {
              new org.jenkins.MetricsPublisher(this).publish(cfg.pushgatewayUrl)
            }
          }
        }
      }
    }

    post {
      success {
        script {
          def log = new org.jenkins.LogHelper(this)
          log.time('GithubNotifier.setCommitStatus [success]') {
            new org.jenkins.GithubNotifier(this).setCommitStatus('jenkins/ci', 'success', 'All actions passed')
          }
        }
      }
      failure {
        script {
          def log = new org.jenkins.LogHelper(this)
          log.time('GithubNotifier.setCommitStatus [failure]') {
            new org.jenkins.GithubNotifier(this).setCommitStatus('jenkins/ci', 'failure', 'Some actions failed')
          }
          log.time('GithubNotifier.postComment [failure]') {
            new org.jenkins.GithubNotifier(this).postComment("⚠️ **Pipeline error** — check [build logs](${env.BUILD_URL}console)")
          }
        }
      }
      always {
        archiveArtifacts artifacts: 'jenkins_pr_metrics.prom', allowEmptyArchive: true
      }
    }
  }
}