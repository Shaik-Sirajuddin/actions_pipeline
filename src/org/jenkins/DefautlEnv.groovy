// src/org/jenkins/DefaultEnv.groovy
package org.jenkins

class DefaultEnv implements Serializable {

  def script

  DefaultEnv(script) {
    this.script = script
  }

  // Injects standard GH context vars that all stage files can rely on
  def inject() {
    // These come from Jenkins GitHub plugin / branch source plugin automatically
    // We normalise them into consistent names here
    script.env.GH_REPO      = script.env.GIT_URL
      ?.replace('https://github.com/', '')
      ?.replace('.git', '')                     // → 'org/repo'

    script.env.GH_SHA       = script.env.GIT_COMMIT          // full commit sha
    script.env.GH_BRANCH    = script.env.CHANGE_BRANCH       // PR source branch
    script.env.GH_PR_NUMBER = script.env.CHANGE_ID           // PR number
    script.env.GH_BASE      = script.env.CHANGE_TARGET       // target branch e.g. main
    script.env.BUILD_URL    = script.env.BUILD_URL            // Jenkins build URL

    script.echo """
      ✅ Default env injected:
         GH_REPO      = ${script.env.GH_REPO}
         GH_SHA       = ${script.env.GH_SHA}
         GH_BRANCH    = ${script.env.GH_BRANCH}
         GH_PR_NUMBER = ${script.env.GH_PR_NUMBER}
    """
  }
}