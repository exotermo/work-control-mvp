# PGP / assinatura de commits — boas práticas

Índice de tópicos a documentar para assinatura de commits e gestão de chaves PGP/GPG neste
projeto — relevante em especial porque o mantenedor opera com **mais de uma conta GitHub** e
precisa que a chave/identidade certa assine cada commit.

## Tópicos

1. **Geração de chave** — tipo (Ed25519 vs RSA), tamanho, data de expiração, por que expirar chaves periodicamente.
2. **Identidade da chave por conta** — uma chave (ou subchave) por conta GitHub, associada ao e-mail correspondente daquela conta.
3. **Armazenamento da chave privada** — uso de `gpg-agent`, cache de senha, opção de token de hardware (YubiKey) para não deixar a chave privada em texto puro no disco.
4. **Configuração do Git para assinar** — `user.signingkey`, `commit.gpgsign`, `tag.gpgSign`, e a diferença para assinatura via SSH key (`gpg.format ssh`).
5. **Troca automática de identidade por diretório** — `git config includeIf "gitdir:..."` para que cada pasta/repositório use a chave e o e-mail da conta correta sem trocar manualmente.
6. **Publicar a chave pública no GitHub** — Settings → SSH and GPG keys, e o que isso habilita (selo "Verified").
7. **Backup e certificado de revogação** — gerar e guardar o certificado de revogação fora da máquina de trabalho no momento da criação da chave, não só quando for tarde demais.
8. **Rotação e revogação de chaves** — quando revogar (perda de acesso, expiração, comprometimento) e como comunicar a mudança.
9. **Verificação de assinaturas** — `git log --show-signature`, e como auditar se o histórico está realmente assinado por quem diz estar.
10. **SSH + múltiplas contas GitHub** — `~/.ssh/config` com `Host` aliases por conta, para casar com a troca de chave de assinatura por repositório.

Cada tópico deve virar sua própria seção (ou arquivo, se crescer) com os comandos exatos adotados
neste projeto — este README é só o índice.
