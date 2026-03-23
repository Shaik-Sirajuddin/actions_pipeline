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

  def fetchTokensFromVault(String vaultUrl, String credentialId, List<String> secrets) {
    def vaultSecretsList = []
    for (int i = 0; i < secrets.size(); i++) {
      def secretName = secrets[i]
      vaultSecretsList.add([
        path: "secret/${script.env.REPO_NAME}/${secretName}",
        secretValues: [
          [envVar: secretName, vaultKey: secretName]
        ]
      ])
    }

    script.withVault(
      configuration: [
        vaultUrl         : vaultUrl,
        vaultCredentialId: credentialId,
        engineVersion    : 2
      ],
      vaultSecrets: vaultSecretsList
    ) {
      for (int i = 0; i < secrets.size(); i++) {
        def secretName = secrets[i]
        script.env."${secretName}" = script.env."${secretName}"
      }
      script.echo "Tokens fetched from Vault successfully"
    }
  }
}