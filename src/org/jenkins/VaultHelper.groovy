package org.jenkins

class VaultHelper implements Serializable {
  def script

  VaultHelper(script) { this.script = script }

  def fetchGithubToken(String vaultUrl, String credentialId, String secretPath) {
    script.withVault(
      configuration: [
        vaultUrl         : vaultUrl,
        vaultCredentialId: credentialId,
        engineVersion    : 2
      ],
      vaultSecrets: [[
        path: secretPath,
        secretValues: [
          [envVar: 'GITHUB_TOKEN', vaultKey: 'token']
        ]
      ]]
    ) {
      script.env.GITHUB_TOKEN = script.env.GITHUB_TOKEN
      script.echo "Secrets fetched from Vault successfully"
    }
  }
}