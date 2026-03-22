package org.jenkins

class GithubNotifier implements Serializable {
  def script

  GithubNotifier(script) { this.script = script }

  def postComment(String body) {
    def payload = groovy.json.JsonOutput.toJson([body: body])
    script.sh """
      set +x
      curl -s -X POST \\
        -H "Authorization: Bearer \${GITHUB_TOKEN}" \\
        -H "Content-Type: application/json" \\
        -d '${payload}' \\
        "https://api.github.com/repos/${script.env.REPO}/issues/${script.env.PR_ID}/comments"
    """
  }

  def setCommitStatus(String context, String state, String description) {
    def sha = script.sh(script: 'git rev-parse HEAD', returnStdout: true).trim()
    def payload = groovy.json.JsonOutput.toJson([
      state      : state,
      description: description,
      context    : "jenkins/${context}",
      target_url : script.env.BUILD_URL
    ])
    script.sh """
      set +x
      curl -s -X POST \\
        -H "Authorization: Bearer \${GITHUB_TOKEN}" \\
        -H "Content-Type: application/json" \\
        -d '${payload}' \\
        "https://api.github.com/repos/${script.env.REPO}/statuses/${sha}"
    """
  }

  def publishComment() {
    def results   = new groovy.json.JsonSlurperClassic().parseText(script.env.ACTION_RESULTS_JSON)
    def failed    = results.count { it.status == 'fail' }
    def totalTime = String.format("%.2f", results.sum { it.execution_time } as double)

    def summary = failed == 0
      ? "## ✅ CI Passed — All ${results.size()} actions succeeded"
      : "## ❌ CI Failed — ${failed}/${results.size()} actions failed"

    def fence = '```'
    def table = """
${summary}

> PR by **@${script.env.PR_AUTHOR}** · Build [#${script.env.BUILD_NUMBER}](${script.env.BUILD_URL}) · Total time: **${totalTime}s**

| # | Action | Status | Duration |
|---|--------|--------|----------|
${results.collect { r ->
  def icon  = r.status == 'pass' ? '✅ pass' : '❌ fail'
  def fname = '`' + r.file_name + '`'
  "| ${r.job_index}/${r.total_jobs} | ${fname} | ${icon} | ${r.execution_time}s |"
}.join('\n')}

<details>
<summary>📋 Full metadata (JSON)</summary>

${fence}json
${groovy.json.JsonOutput.prettyPrint(script.env.ACTION_RESULTS_JSON)}
${fence}
</details>
"""
    postComment(table)
  }
}