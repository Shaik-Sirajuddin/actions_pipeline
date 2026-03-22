package org.jenkins

class ActionRunner implements Serializable {
  def script

  ActionRunner(script) { this.script = script }

  def discover(String actionsDir) {
    def raw = script.sh(
      script: "find ${actionsDir} -type f | sort",
      returnStdout: true
    ).trim()

    if (!raw) {
      script.error("No actions found in ${actionsDir}")
    }

    script.env.ACTION_FILES = raw
    script.env.TOTAL_JOBS   = raw.split('\n').size().toString()
    script.echo "Found ${script.env.TOTAL_JOBS} actions"

    new GithubNotifier(script).postComment(
      "🔄 **Jenkins CI started** — found **${script.env.TOTAL_JOBS}** actions to run."
    )
  }

  def runAll() {
    def actionFiles = script.env.ACTION_FILES.split('\n')
    def results     = []
    def notifier    = new GithubNotifier(script)

    actionFiles.eachWithIndex { filePath, idx ->
      def jobIndex  = idx + 1
      def fileName  = filePath.tokenize('/').last()
      def startTime = System.currentTimeMillis()
      def status    = 'pass'

      script.echo "▶ Running action ${jobIndex}/${script.env.TOTAL_JOBS}: ${fileName}"
      notifier.setCommitStatus(fileName, 'pending', "Running ${fileName}...")

      try {
        def exitCode = script.sh(
          script: "chmod +x ${filePath} && ${filePath}",
          returnStatus: true
        )
        status = (exitCode == 0) ? 'pass' : 'fail'
      } catch (err) {
        status = 'fail'
      }

        def executionTime = Double.parseDouble(
        String.format("%.2f", (System.currentTimeMillis() - startTime) / 1000.0)
      )

      results.add([
        job_id        : script.env.JOB_ID,
        github        : [
          repository  : script.env.REPO,
          pr_id       : script.env.PR_ID,
          pr_author   : script.env.PR_AUTHOR
        ],
        file_name     : fileName,
        execution_time: executionTime,
        status        : status,
        job_index     : jobIndex,
        total_jobs    : script.env.TOTAL_JOBS.toInteger()
      ])

      notifier.setCommitStatus(
        fileName,
        status == 'pass' ? 'success' : 'failure',
        "${fileName} ${status} in ${executionTime}s"
      )
      script.echo "${status == 'pass' ? '✅' : '❌'} ${fileName} — ${status} (${executionTime}s)"
    }

    script.env.ACTION_RESULTS_JSON = groovy.json.JsonOutput.toJson(results)
  }
}