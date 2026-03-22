package org.jenkins

class LogHelper implements Serializable {
  def script

  LogHelper(script) { this.script = script }

  def time(String label, Closure body) {
    def start = System.currentTimeMillis()
    script.echo "▶ [START] ${label}"
    try {
      def result = body()
      def elapsed = ((System.currentTimeMillis() - start) / 1000).round(2)
      script.echo "✅ [DONE]  ${label} — ${elapsed}s"
      return result
    } catch (err) {
      def elapsed = ((System.currentTimeMillis() - start) / 1000).round(2)
      script.echo "❌ [FAIL]  ${label} — ${elapsed}s — ${err.message}"
      throw err
    }
  }
}