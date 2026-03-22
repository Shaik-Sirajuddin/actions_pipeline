package org.jenkins

class GithubNotifier implements Serializable {
  def script

  GithubNotifier(script) { this.script = script }

  def postComment(String body) {
    def payload = groovy.json.JsonOutput.toJson([body: body])
    script.sh """
      curl -s -X POST \
        -H "Authorization: token ${script.env.GITHUB_TOKEN}" \
        -H "Content-Type: application/json" \
        -d '${payload}' \
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
      curl -s -X POST \
        -H "Authorization: token ${script.env.GITHUB_TOKEN}" \
        -H "Content-Type: application/json" \
        -d '${payload}' \
        "https://api.github.com/repos/${script.env.REPO}/statuses/${sha}"
    """
  }

  def publishComment() {
    def results   = new groovy.json.JsonSlurper().parseText(script.env.ACTION_RESULTS_JSON)
    def passed    = results.count { it.status == 'pass' }
    def failed    = results.count { it.status == 'fail' }
    def totalTime = results.sum { it.execution_time }.round(2)

    def summary = failed == 0
      ? "## ✅ CI Passed — All ${results.size()} actions succeeded"
      : "## ❌ CI Failed — ${failed}/${results.size()} actions failed"

    def table = """
${summary}

> PR by **@${script.env.PR_AUTHOR}** · Build [#${script.env.BUILD_NUMBER}](${script.env.BUILD_URL}) · Total time: **${totalTime}s**

| # | Action | Status | Duration |
|---|--------|--------|----------|
${results.collect { r ->
  def icon = r.status == 'pass' ? '✅ pass' : '❌ fail'
  "| ${r.job_index}/${r.total_jobs} | \`${r.file_name}\` | ${icon} | ${r.execution_time}s |"
}.join('\n')}

<details>
<summary>📋 Full metadata (JSON)</summary>

\`\`\`json
${groovy.json.JsonOutput.prettyPrint(script.env.ACTION_RESULTS_JSON)}
\`\`\`
</details>
"""
    postComment(table)
  }
}