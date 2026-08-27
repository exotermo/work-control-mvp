# 0004 — Schema Postgres + api-go real (Fase A do plano de dados)

Data: 2026-08-21
Status: pendente

## Resumo

Implementada a Fase A do plano `banco de dados real + contrato de API + Android sem mocks`
(`~/.claude/plans/prancy-mapping-lark.md`): as 15 tabelas do README viraram schema real
(`golang-migrate`), `sqlc` gera o código Go tipado a partir das queries, e o `api-go` ganhou
handlers REST + WebSocket de verdade para tasks/agents/devices/approvals — tudo verificado
rodando (migrations aplicadas, seed idempotente reproduzindo `FakeData.kt` byte-a-byte para a
tarefa #184, endpoints testados via `curl`, WebSocket testado com um cliente Go descartável
recebendo eventos ao vivo do simulador de demonstração, build de imagem Docker e stack completa
via `docker compose` — postgres/migrate/api/seed — todos verificados).

A autenticação OIDC real (`internal/auth/`) e a Fase B do Android (repositórios reais) ficam
registradas em separado, em `0005-oidc-authentication.md` e `0006-android-real-api-integration.md`.

## Arquivos tocados

- `services/api-go/migrations/000001_init.{up,down}.sql` (novo) — schema completo
- `services/api-go/sqlc.yaml`, `internal/store/queries/*.sql` (exceto `auth.sql`),
  `internal/store/*.go` (gerado, exceto `auth.sql.go`) (novo)
- `services/api-go/internal/{config,devidentity,devseed,audit,httpx,workspace,tasks,agents,devices,approvals,events,health}/` (novo)
- `services/api-go/cmd/api/main.go`, `services/api-go/cmd/seed/main.go`, `services/api-go/cmd/ws-smoke/main.go` (novo)
- `services/api-go/Dockerfile` (novo)
- `services/api-go/go.mod`, `go.sum` — pgx/v5, google/uuid, coder/websocket
- `infra/docker/docker-compose.yml` — serviços `migrate`, `api`, `seed` + healthcheck no postgres
- `.env.example` — `HTTP_PORT`, `SEED_DEV_DATA`, `DEMO_SIMULATOR`

## Mensagem de commit sugerida

```
feat(api-go): schema Postgres real + API REST/WebSocket, substitui os mocks do README

golang-migrate + sqlc, sem ORM. Handlers para tasks/agents/devices/approvals
espelhando os 4 repositórios do Android. WebSocket por tarefa (coder/websocket)
com simulador de demonstração opcional (DEMO_SIMULATOR). Seed idempotente
reproduz a história do bug #184 usada em FakeData.kt. Docker Compose sobe
postgres → migrate → api. Tudo verificado rodando neste ambiente.
```
