package org.jenkins

class GithubNotifier implements Serializable {
  def script

  GithubNotifier(script) { this.script = script }

  def postComment(String body) {
    script.pullRequest.comment(body)
  }

  def setCommitStatus(String context, String state, String description) {
    script.githubNotify(
      credentialsId: 'github-credentials',
      context      : "jenkins/${context}",
      status       : state.toUpperCase(),
      description  : description,
      targetUrl    : script.env.BUILD_URL
    )
  }

  def publishComment() {
    def results   = new groovy.json.JsonSlurper().parseText(script.env.ACTION_RESULTS_JSON)
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