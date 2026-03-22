package org.jenkins

class LogHelper implements Serializable {
  def script

  LogHelper(script) { this.script = script }

  def time(String label, Closure body) {
    def start = System.currentTimeMillis()
    script.echo "▶ [START] ${label}"
    try {
      def result = body()
      def elapsed = (System.currentTimeMillis() - start) / 1000.0
      // ❌ .round(2) doesn't work in CPS sandbox
      // ✅ use String.format instead
      def elapsedStr = String.format("%.2f", elapsed)
      script.echo "✅ [DONE]  ${label} — ${elapsedStr}s"
      return result
    } catch (err) {
      def elapsed = (System.currentTimeMillis() - start) / 1000.0
      def elapsedStr = String.format("%.2f", elapsed)
      script.echo "❌ [FAIL]  ${label} — ${elapsedStr}s — ${err.message}"
      throw err
    }
  }
}