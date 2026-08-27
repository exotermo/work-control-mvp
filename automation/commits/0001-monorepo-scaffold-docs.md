# 0001 — Scaffold do monorepo, documentação e specs de produto

Data: 2026-08-27
Status: pendente

## Resumo

Primeiro commit do monorepo: README raiz, `CLAUDE.md`, convenções de workflow (`.gitignore`,
`.sdkmanrc`, CI placeholder), documentação de arquitetura/produto/segurança/runbooks, os
acompanhamentos do projeto (`PLANO_DE_ACAO_MVP.md`, `CHECKLIST_MVP.md`, `DIARIO_DE_BORDO.md`),
o artigo de divulgação, e os placeholders ainda não implementados (`device-agent-go`,
`orchestrator-java`, infra de Kubernetes/NGINX/fail2ban/Cloudflare).

## Arquivos tocados

- `README.md`, `CLAUDE.md`, `.gitignore`, `.sdkmanrc`, `.github/workflows/ci.yml`
- `PLANO_DE_ACAO_MVP.md`, `CHECKLIST_MVP.md`, `DIARIO_DE_BORDO.md`, `ARTIGO_LINKEDIN_WORK_CONTROL.md`
- `automation/commits/README.md`, `automation/scripts/verify-local.sh`
- `docs/adr/0001-monorepo.md`, `docs/architecture/README.md`
- `docs/product/mvp-scope.md`, `docs/product/prioritized-user-journey.md`,
  `docs/product/screen-navigation-map.md`, `docs/product/ui-state-contract.md`
- `docs/runbooks/local-development.md`
- `docs/pgp/README.md`, `docs/sdd/README.md`
- `docs/security/README.md`, `docs/security/threat-model-initial.md`
- `infra/cloudflare/README.md`, `infra/cloudflare/tunnel-config.example.yml`
- `infra/fail2ban/README.md`
- `infra/k8s/base/README.md`, `infra/k8s/overlays/homolog/README.md`, `infra/k8s/overlays/prod/README.md`
- `infra/nginx/README.md`
- `services/device-agent-go/` (placeholder)
- `services/orchestrator-java/` (placeholder, sem Gradle Wrapper)

## Mensagem de commit sugerida

```
chore: scaffold do monorepo, documentação e specs de produto

README, CLAUDE.md e convenções de workflow (log de commits em vez de
commit direto, por troca de conta GitHub). Docs de arquitetura, produto,
segurança e runbooks. Placeholders para device-agent-go, orchestrator-java
e infra (k8s/nginx/fail2ban/cloudflare) ainda não implementados.
```
