package org.jenkins

class MetricsPublisher implements Serializable {
  def script

  MetricsPublisher(script) { this.script = script }

  def publish(String pushgatewayUrl) {
    def results = new groovy.json.JsonSlurperClassic().parseText(script.env.ACTION_RESULTS_JSON)
    def passed  = results.count { it.status == 'pass' }
    def failed  = results.count { it.status == 'fail' }
    def labels  = "repository=\"${script.env.REPO}\",pr_id=\"${script.env.PR_ID}\",pr_author=\"${script.env.PR_AUTHOR}\",job_id=\"${script.env.JOB_ID}\""

    def lines = []
    lines << "# HELP jenkins_pr_action_duration_sec onds Execution time per action"
    lines << "# TYPE jenkins_pr_action_duration_seconds gauge"
    lines << "# HELP jenkins_pr_action_status Status per action (1=pass, 0=fail)"
    lines << "# TYPE jenkins_pr_action_status gauge"
    lines << "# HELP jenkins_pr_total_actions Total actions in this run"
    lines << "# TYPE jenkins_pr_total_actions gauge"
    lines << "# HELP jenkins_pr_passed_actions Passed actions"
    lines << "# TYPE jenkins_pr_passed_actions gauge"
    lines << "# HELP jenkins_pr_failed_actions Failed actions"
    lines << "# TYPE jenkins_pr_failed_actions gauge"

    results.each { r ->
      def actionLabels = "${labels},file_name=\"${r.file_name}\",job_index=\"${r.job_index}\""
      lines << "jenkins_pr_action_duration_seconds{${actionLabels}} ${r.execution_time}"
      lines << "jenkins_pr_action_status{${actionLabels}} ${r.status == 'pass' ? 1 : 0}"
    }

    lines << "jenkins_pr_total_actions{${labels}} ${results.size()}"
    lines << "jenkins_pr_passed_actions{${labels}} ${passed}"
    lines << "jenkins_pr_failed_actions{${labels}} ${failed}"

    def metricsContent = lines.join('\n') + '\n'
    script.writeFile file: script.env.METRICS_FILE, text: metricsContent

    // Publish metrics through logger as requested
    script.echo "--- PROMETHEUS METRICS ---"
    script.echo metricsContent
    script.echo "--------------------------"

    // Still attempt push to pushgateway if defined
    if (pushgatewayUrl) {
      try {
        script.sh """
          curl -s --data-binary @${script.env.METRICS_FILE} \
            ${pushgatewayUrl}/metrics/job/jenkins_pr/instance/${script.env.JOB_ID}
        """
        script.echo "Metrics pushed to Pushgateway at ${pushgatewayUrl}"
      } catch (Exception e) {
        script.echo "Failed to push to Pushgateway: ${e.message}"
      }
    }
  }
}