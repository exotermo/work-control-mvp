#!/usr/bin/env bash
# Sequência de commits para o primeiro histórico do repositório, gerada a partir dos logs
# em automation/commits/000{1..6}-*.md. Revise antes de rodar — este script NÃO é executado
# automaticamente pelo assistente, por pedido explícito do mantenedor.
#
# Pré-requisitos já feitos nesta sessão:
#   - git init (branch "main")
#   - git config user.name "Alexandre Tognato" / user.email alexandretognato@hotmail.com
#     (via includeIf de ~/.gitconfig, escopado só para esta pasta)
#   - git remote add origin git@github.com-personal:exotermo/work-control-mvp.git
#   - repositório vazio "work-control-mvp" criado manualmente em github.com/exotermo
#
# Uso: revise, ajuste o que quiser, depois `bash automation/commits/apply-commits.sh`
# Depois de aplicar, rode `git push -u origin main` você mesmo.

set -euo pipefail
cd "$(git rev-parse --show-toplevel)"

# 0001 — scaffold do monorepo, documentação e specs de produto
git add \
  README.md CLAUDE.md .gitignore .sdkmanrc .github/workflows/ci.yml \
  PLANO_DE_ACAO_MVP.md CHECKLIST_MVP.md DIARIO_DE_BORDO.md ARTIGO_LINKEDIN_WORK_CONTROL.md \
  automation/commits/README.md automation/commits/0001-monorepo-scaffold-docs.md \
  automation/commits/apply-commits.sh automation/scripts/verify-local.sh \
  docs/adr/0001-monorepo.md docs/architecture/README.md \
  docs/product/mvp-scope.md docs/product/prioritized-user-journey.md \
  docs/product/screen-navigation-map.md docs/product/ui-state-contract.md \
  docs/runbooks/local-development.md docs/pgp/README.md docs/sdd/README.md \
  docs/security/README.md docs/security/threat-model-initial.md \
  infra/cloudflare/README.md infra/cloudflare/tunnel-config.example.yml \
  infra/fail2ban/README.md \
  infra/k8s/base/README.md infra/k8s/overlays/homolog/README.md infra/k8s/overlays/prod/README.md \
  infra/nginx/README.md \
  services/device-agent-go services/orchestrator-java
git commit -m "chore: scaffold do monorepo, documentação e specs de produto

README, CLAUDE.md e convenções de workflow (log de commits em vez de
commit direto, por troca de conta GitHub). Docs de arquitetura, produto,
segurança e runbooks. Placeholders para device-agent-go, orchestrator-java
e infra (k8s/nginx/fail2ban/cloudflare) ainda não implementados."

# 0002 — export Figma de referência
git add "Work Control Mobile Prototype" automation/commits/0002-figma-prototype-export.md
git commit -m "chore(design): adiciona export Figma de referência (Work Control Mobile Prototype)

Protótipo React/Vite/shadcn aprovado, usado como referência visual e
funcional para a implementação nativa em apps/android. Material de
design, não faz parte do runtime do monorepo."

# 0003 — scaffold Android nativo (Kotlin/Compose, MVVM/MVI)
git add \
  apps/android/settings.gradle.kts apps/android/build.gradle.kts apps/android/gradle.properties \
  apps/android/gradle/libs.versions.toml apps/android/gradle/wrapper/gradle-wrapper.jar \
  apps/android/gradle/wrapper/gradle-wrapper.properties apps/android/gradlew apps/android/gradlew.bat \
  apps/android/local.properties.example \
  apps/android/app/build.gradle.kts apps/android/app/proguard-rules.pro \
  apps/android/app/src/main/AndroidManifest.xml apps/android/app/src/debug/AndroidManifest.xml \
  apps/android/app/src/main/res \
  apps/android/app/src/main/kotlin/com/workcontrol/app/MainActivity.kt \
  apps/android/app/src/main/kotlin/com/workcontrol/app/WorkControlApp.kt \
  apps/android/app/src/main/kotlin/com/workcontrol/app/core/designsystem \
  apps/android/app/src/main/kotlin/com/workcontrol/app/core/components/BottomNavBar.kt \
  apps/android/app/src/main/kotlin/com/workcontrol/app/core/components/PlaceholderScreen.kt \
  apps/android/app/src/main/kotlin/com/workcontrol/app/core/components/ProgressBar.kt \
  apps/android/app/src/main/kotlin/com/workcontrol/app/core/components/QuickActionsSheet.kt \
  apps/android/app/src/main/kotlin/com/workcontrol/app/core/components/StatusDot.kt \
  apps/android/app/src/main/kotlin/com/workcontrol/app/core/components/SurfaceCard.kt \
  apps/android/app/src/main/kotlin/com/workcontrol/app/core/components/Text.kt \
  apps/android/app/src/main/kotlin/com/workcontrol/app/core/components/WorkControlTopBar.kt \
  apps/android/app/src/main/kotlin/com/workcontrol/app/navigation \
  apps/android/app/src/main/kotlin/com/workcontrol/app/domain/model/Models.kt \
  apps/android/app/src/main/kotlin/com/workcontrol/app/domain/repository/Repositories.kt \
  apps/android/app/src/main/kotlin/com/workcontrol/app/domain/usecase/UseCases.kt \
  apps/android/app/src/main/kotlin/com/workcontrol/app/feature \
  apps/android/app/src/androidTest \
  apps/android/app/src/test/kotlin/com/workcontrol/app/MainDispatcherRule.kt \
  apps/android/app/src/test/kotlin/com/workcontrol/app/feature/home/HomeViewModelTest.kt \
  apps/android/app/src/test/kotlin/com/workcontrol/app/feature/tasks/TaskListViewModelTest.kt \
  apps/android/app/src/test/kotlin/com/workcontrol/app/domain/usecase/CreateTaskUseCaseTest.kt \
  automation/commits/0003-android-kotlin-scaffold.md
git commit -m "feat(android): scaffold do app nativo Kotlin/Compose a partir do protótipo Figma

Clean Architecture + MVVM/MVI, Hilt/KSP, Navigation Compose type-safe.
Home, Tarefas e Detalhe da Tarefa portados por completo com dados fake
reativos; demais 11 telas como placeholders navegáveis. Build debug e
testes unitários (5/5) verificados neste ambiente."

# 0004 — schema Postgres + api-go real (REST/WebSocket)
git add \
  services/api-go/migrations/000001_init.up.sql services/api-go/migrations/000001_init.down.sql \
  services/api-go/sqlc.yaml \
  services/api-go/internal/store/queries/agents.sql services/api-go/internal/store/queries/approvals.sql \
  services/api-go/internal/store/queries/audit.sql services/api-go/internal/store/queries/devices.sql \
  services/api-go/internal/store/queries/seed.sql services/api-go/internal/store/queries/simulator.sql \
  services/api-go/internal/store/queries/tasks.sql services/api-go/internal/store/queries/workspaces.sql \
  services/api-go/internal/store/agents.sql.go services/api-go/internal/store/approvals.sql.go \
  services/api-go/internal/store/audit.sql.go services/api-go/internal/store/db.go \
  services/api-go/internal/store/devices.sql.go services/api-go/internal/store/models.go \
  services/api-go/internal/store/querier.go services/api-go/internal/store/seed.sql.go \
  services/api-go/internal/store/simulator.sql.go services/api-go/internal/store/tasks.sql.go \
  services/api-go/internal/store/workspaces.sql.go \
  services/api-go/internal/config services/api-go/internal/devidentity services/api-go/internal/devseed \
  services/api-go/internal/audit services/api-go/internal/httpx services/api-go/internal/workspace \
  services/api-go/internal/tasks services/api-go/internal/agents services/api-go/internal/devices \
  services/api-go/internal/approvals services/api-go/internal/events services/api-go/internal/health \
  services/api-go/cmd/api/main.go services/api-go/cmd/seed/main.go services/api-go/cmd/ws-smoke/main.go \
  services/api-go/Dockerfile services/api-go/go.mod services/api-go/go.sum \
  infra/docker/docker-compose.yml .env.example \
  automation/commits/0004-postgres-schema-apigo-backend.md
git commit -m "feat(api-go): schema Postgres real + API REST/WebSocket, substitui os mocks do README

golang-migrate + sqlc, sem ORM. Handlers para tasks/agents/devices/approvals
espelhando os 4 repositórios do Android. WebSocket por tarefa (coder/websocket)
com simulador de demonstração opcional (DEMO_SIMULATOR). Seed idempotente
reproduz a história do bug #184 usada em FakeData.kt. Docker Compose sobe
postgres → migrate → api. Tudo verificado rodando neste ambiente."

# 0005 — autenticação OIDC real
git add \
  services/api-go/internal/auth \
  services/api-go/internal/store/auth.sql.go services/api-go/internal/store/queries/auth.sql \
  services/api-go/migrations/000002_external_identities.up.sql \
  services/api-go/migrations/000002_external_identities.down.sql \
  infra/keycloak/realm-work-control-local.json automation/scripts/oidc-local-smoke.py \
  docs/adr/0002-provider-neutral-oidc.md docs/security/oidc-environment-matrix.md \
  automation/commits/0005-oidc-authentication.md
git commit -m "feat(api-go): autenticação OIDC real (Discovery/JWKS, identidades externas, /v1/me)

AUTH_MODE=oidc ao lado do dev existente. Verifica token via Discovery
Document + JWKS, vincula identidade externa a um usuário local, expõe
GET /v1/me. Keycloak de referência local só para o profile Compose
oidc; nunca reutilizar essas credenciais fora de dev."

# 0006 — Android consome a API real, remove mocks do runtime
git add \
  apps/android/app/src/main/kotlin/com/workcontrol/app/data/remote \
  apps/android/app/src/main/kotlin/com/workcontrol/app/data/auth \
  apps/android/app/src/main/kotlin/com/workcontrol/app/data/di \
  apps/android/app/src/main/kotlin/com/workcontrol/app/domain/error \
  apps/android/app/src/main/kotlin/com/workcontrol/app/core/components/RemoteStatePanel.kt \
  apps/android/app/src/test/kotlin/com/workcontrol/app/data \
  docs/adr/0003-android-retrofit-cloudflare-edge.md \
  automation/commits/0006-android-real-api-integration.md
git commit -m "feat(android): integra API real via Retrofit/OkHttp/WebSocket, remove mocks do runtime

Os 4 repositórios passam a falar com o api-go real (REST + WS
reconectável) em vez de Fake* em memória. Interceptor Bearer com
fronteira de token em memória (login/sessão persistente ainda
pendente). Home/Tarefas/Detalhe ganham loading/vazio/erro reais.
Fake* migram para src/test, mesmo pacote — testes existentes intactos."

echo "Pronto: 6 commits criados em 'main'. Confira com 'git log --oneline' antes de:"
echo "  git push -u origin main"
