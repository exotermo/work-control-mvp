# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project status

This is a **partially executable MVP baseline**, not merely a monorepo skeleton. The architecture in `README.md` remains the target design, while the current implementation is:

- `apps/android`: native Kotlin/Compose project with Gradle Wrapper, Hilt/KSP, typed Navigation Compose, design tokens/components, Retrofit 2.12/Gson, OkHttp 4.12 REST/WebSocket, domain/repository abstractions and remote runtime repositories. Home, task list and task detail consume the Go API with loading/empty/error states; fakes exist only under `src/test`. Most secondary routes remain placeholders. The bottom navigation includes Android system-bar insets, and the four semantic navigation tests previously passed on an MC2200 small-screen device. Visual review remains human-owned.
- `services/api-go`: Go 1.25 API backed by PostgreSQL via pgx/sqlc. It contains migrations, idempotent development seed, REST handlers, WebSocket hub/simulator, healthchecks, real tests, explicit `dev`/`oidc` auth modes, OIDC Discovery/JWKS verification, external identities and `GET /v1/me`.
- `infra/docker/docker-compose.yml`: starts PostgreSQL 17, Redis 7, migrations, API and optional seed. Keycloak 26.7.1 is an optional `oidc` profile. Host ports bind only to loopback; the Compose API intentionally runs with `AUTH_MODE=dev`.
- `services/device-agent-go`: still a placeholder executable; intended packages are not implemented.
- `services/orchestrator-java`: still a placeholder Java 21 application plus placeholder JUnit test, with no checked-in Gradle Wrapper.
- `.github/workflows/ci.yml`, Kubernetes, NGINX and fail2ban remain placeholders/documentation. Cloudflare has a credential-free Tunnel example for `work-control.xandehome.api.br`, not a live route.
- Android has a Bearer interceptor and in-memory token boundary, but no login/protected persistent session implementation yet. OIDC is implemented and smoke-tested on the provider/API side. Resource handlers do **not** yet enforce membership/workspace isolation; never expose the prepared Cloudflare route or treat valid authentication as authorization.

The active work is tracked in `PLANO_DE_ACAO_MVP.md`, `CHECKLIST_MVP.md` and `DIARIO_DE_BORDO.md`. Update the checklist and diary after every implementation/validation.

The `.git` directory is currently empty. **Do not run `git init`, create commits, set remotes or change Git identity** until the maintainer confirms dual Git setup on the company computer is complete.

## Commands

There is no top-level build system tying every service together. The canonical baseline verifier covers Android, API Go and Compose syntax:

```bash
sdk env
./automation/scripts/verify-local.sh
```

**API Go** (`services/api-go`), Go 1.25:
```bash
cd services/api-go && go build ./... && go test ./...
go test ./... -run TestName    # single test
go run ./cmd/ws-smoke          # local WebSocket handshake smoke test
```

`internal/auth`, `internal/config`, `internal/events` and `internal/health` have unit tests. `internal/tasks` has a PostgreSQL integration test gated by `TEST_DATABASE_URL`; use the command in the local runbook so it is not silently skipped.

**Device agent placeholder** (`services/device-agent-go`):

```bash
cd services/device-agent-go && go build ./... && go test ./...
```

**Orchestrator** (`services/orchestrator-java`), Java 21 toolchain declared in `build.gradle.kts`, JUnit 5:
```bash
cd services/orchestrator-java && gradle test    # no gradlew checked in — needs a local Gradle install
gradle test --tests "com.workcontrol.orchestrator.ApplicationTest"   # single test
```
Use a complete JDK, not a JRE. The root `.sdkmanrc` selects `21.0.2-tem`. The environment's `/usr/lib/jvm/java-21-openjdk` may lack `jlink`; see `docs/runbooks/local-development.md`.

**Android** (`apps/android`):

```bash
cd apps/android
./gradlew testDebugUnitTest assembleDebug
./gradlew :app:assembleDebugAndroidTest
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

**Local infra:**
```bash
docker compose -f infra/docker/docker-compose.yml --profile dev up -d --build
curl --fail http://localhost:8080/health/ready
```

The current Compose file uses explicit local-only credentials. Never reuse them outside local development. The canonical commands, physical-device instrumentation command and the real PKCE smoke are in `docs/runbooks/local-development.md`.

## Architecture

Work Control is a mobile "cockpit" for supervising AI coding agents running on remote machines — not a chatbot. The core object graph is `Workspace → Project → Task → Agent → Execution → Artifact`, and every execution screen is meant to answer: what's happening, who's doing it, where it's running, what was the result, does it need me.

Target system shape (see `README.md` for full detail in Portuguese):

```
Android App (Kotlin) → Cloudflare Zero Trust → NGINX → api-go (Go) ⇄ Redis
                                                            ↓
                                                  orchestrator-java (Java 21)
                                                            ↓
                                                       PostgreSQL
                                                            ↓
                                              device-agent-go (Go) → PC/Server/SSH/Docker/Git
```

- **`services/api-go`** — the gateway: REST/gRPC/WebSocket, auth, workspaces/projects, tasks, devices, approvals, artifacts, event streaming. Talks to Redis for cache/sessions/presence/locks/rate-limiting, and to the orchestrator for task execution.
- **`services/orchestrator-java`** — workflow brain: task decomposition, agent selection, step dependencies, a state machine per execution, approval policies, retries/timeout/cancellation, adapters to different AI providers, execution auditing. PostgreSQL is its source of truth. Expected to lean on Java 21 virtual threads for concurrent I/O.
- **`services/device-agent-go`** — installed on authorized target machines; announces capabilities (e.g. `terminal`, `filesystem`, `git`, `docker`, `screen`) and executes work under least privilege. Never assume a capability is available — it must be explicitly authorized per device/agent.
- **`apps/android`** — the mobile client (Kotlin, Clean Architecture-style domain/repository boundaries, MVVM, Jetpack Compose, Coroutines/Flow). Retrofit/OkHttp REST and authenticated/reconnecting WebSocket are implemented; OIDC login and durable protected session are still pending.
- **PostgreSQL** is the persistent source of truth (users, workspaces, projects, devices, agents, tasks, task_steps, executions, execution_events, approvals, artifacts, credentials_metadata, audit_logs). Real secrets/credentials must never be stored in plaintext in these tables.
- **Redis** is strictly for ephemeral state (cache, short sessions, device heartbeat/presence, distributed locks, rate limiting, transient stream state) — never a system of record.

### Security model (central to this product, not an afterthought)

- Location/network presence is only a *hint* for suggesting a workspace — it must never be treated as authentication or authorization.
- Agents get capability-scoped permissions (e.g. a Dev agent can read/write code and run tests but cannot deploy to prod or read secrets; a DevOps agent can touch staging but production requires human approval).
- Any sensitive operation (deploy, destructive change, prod access, credential use, privileged commands) must create an `Approval` and block until a human approves it — this is not optional per-feature behavior, it's a system invariant.
- Cloudflare Zero Trust guards admin/internal endpoints; being on the "right" network is never sufficient trust on its own.
- Every meaningful action should produce an audit event (user, agent, device, abstract command/action, timestamp, result, associated approval if any). Secrets must never be written to logs.

### Environments

Current local Compose is Postgres + Redis + migrations + API + optional seed; the orchestrator is not wired yet. Target progression remains Local → Homologação (isolated k8s namespace/cluster, own domain/secrets, sandbox integrations, integration/E2E gate) → Produção (isolated secrets, backups, network policies, resource limits, probes, autoscaling, centralized logs/metrics, rollback). Kubernetes config lives under `infra/k8s/base` with `infra/k8s/overlays/{homolog,prod}` for Kustomize patches. Never auto-promote destructive migrations or critical policy changes without an explicit gate.
