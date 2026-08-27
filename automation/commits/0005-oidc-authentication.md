# 0005 — Autenticação OIDC no api-go

Data: 2026-08-27
Status: pendente

## Resumo

Modo de autenticação `oidc` real ao lado do `dev` (placeholder) já existente: Discovery/JWKS,
verificação de token, identidades externas vinculadas a `users`, e `GET /v1/me`. Inclui o Keycloak
26.7.1 de referência local (profile Compose `oidc`, credenciais inseguras por definição, nunca fora
de uma máquina de desenvolvimento) e o smoke test de PKCE real contra ele.

## Arquivos tocados

- `services/api-go/internal/auth/auth.go`, `auth_test.go`
- `services/api-go/internal/auth/oidc.go`, `oidc_test.go`
- `services/api-go/internal/auth/me.go`, `me_test.go`
- `services/api-go/internal/auth/store.go`, `store_test.go`
- `services/api-go/internal/store/auth.sql.go` (gerado), `internal/store/queries/auth.sql`
- `services/api-go/migrations/000002_external_identities.{up,down}.sql`
- `infra/keycloak/realm-work-control-local.json`
- `automation/scripts/oidc-local-smoke.py`
- `docs/adr/0002-provider-neutral-oidc.md`, `docs/security/oidc-environment-matrix.md`

## Mensagem de commit sugerida

```
feat(api-go): autenticação OIDC real (Discovery/JWKS, identidades externas, /v1/me)

AUTH_MODE=oidc ao lado do dev existente. Verifica token via Discovery
Document + JWKS, vincula identidade externa a um usuário local, expõe
GET /v1/me. Keycloak de referência local só para o profile Compose
oidc; nunca reutilizar essas credenciais fora de dev.
```
