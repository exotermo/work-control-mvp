# Checklist de Execução — Work Control MVP

**Atualizado em:** 2026-08-24  
**Marco ativo:** Marco 3 implementado no ambiente local; login Android, autorização BOLA, revisão humana e Git preservados

Use este arquivo como lista operacional. Após cada implementação, atualizar o item, incluir a evidência e registrar a execução no `DIARIO_DE_BORDO.md`.

## Marco 0 — baseline reproduzível

### Rastreabilidade

- [x] Criar `PLANO_DE_ACAO_MVP.md`.
- [x] Criar este checklist.
- [x] Criar `DIARIO_DE_BORDO.md`.
- [x] Preparar artigo do LinkedIn.
- [ ] ⏸ Restaurar ou inicializar o Git — aguardando configuração de dual Git pelo mantenedor.
- [ ] ⏸ Configurar identidade, assinatura e remoto corretos — ação deliberadamente adiada.
- [ ] Registrar a baseline existente no primeiro commit após a configuração Git.

### Android

- [x] Confirmar Gradle Wrapper e configuração do projeto Android.
- [x] Localizar JDK 21 completo disponível.
- [x] Executar `testDebugUnitTest` com sucesso.
- [x] Executar `assembleDebug` com sucesso.
- [x] Confirmar geração de `app-debug.apk`.
- [x] Adicionar configuração de versão Java reproduzível ao projeto.
- [x] Documentar comandos de build, teste, instalação e troubleshooting.
- [x] Detectar aparelho físico ou emulador via ADB.
- [x] Instalar o APK.
- [x] Abrir o aplicativo e validar ausência de crash na inicialização.
- [x] Navegar por Home, Tarefas e detalhe da tarefa `#184`.
- [x] Corrigir sobreposição da bottom bar com a navegação de três botões do Android.
- [x] Registrar telas completas, placeholders e problemas encontrados no diário.
- [x] Validar Home, Tarefas/detalhe, Máquinas, Arquivos e Nova Tarefa por smoke instrumentado no MC2200.
- [ ] Cobrir Agent, Approval, CodeDiff, Result, Terminal e detalhe de máquina no smoke instrumentado.
- [ ] 👤 Revisar visualmente as rotas placeholder e a composição em telas pequenas.

**Evidência atual:** unit tests, APK debug e APK de instrumentação terminaram com `BUILD SUCCESSFUL`; `MainActivityNavigationTest` executou `4/4`, sem skips/falhas, no MC2200 e não usa coordenadas. O Samsung interrompeu a hierarquia Compose ao perder foreground no harness; nenhuma captura ou tentativa de desbloqueio foi feita.

### API Go e infraestrutura

- [x] Executar `go test ./...` com sucesso de compilação.
- [x] Adicionar testes automatizados reais aos pacotes da API.
- [x] Validar `docker compose config`.
- [x] Subir PostgreSQL e Redis.
- [x] Aplicar migrations.
- [x] Subir a API.
- [x] Executar seed de desenvolvimento.
- [x] Validar endpoint de workspaces.
- [x] Validar dashboard.
- [x] Validar listagem de tarefas.
- [x] Validar criação de tarefa por teste transacional contra PostgreSQL real, com rollback.
- [x] Validar handshake e encerramento do WebSocket com cliente local permanente.
- [x] Validar recebimento determinístico de evento com fixture direta do hub WebSocket.
- [x] Definir e implementar liveness/readiness da API.
- [x] Adicionar testes automatizados do healthcheck.
- [x] Adicionar healthcheck da API ao Docker Compose.

**Evidência atual:** `TEST_DATABASE_URL=... go test -count=1 ./...`, `go vet ./...` e `go test -race ./internal/events ./internal/auth` passaram. A migration `000002` está em versão `2`, `dirty=false`.

### Documentação

- [x] Criar runbook de desenvolvimento local.
- [x] Atualizar `README.md` com quickstart e estado atual real.
- [x] Atualizar `CLAUDE.md` com a implementação existente.
- [x] Documentar o problema de `JAVA_HOME` apontando para JRE sem `jlink`.
- [x] Documentar como executar o APK em aparelho e emulador.
- [x] Revisar se nenhum formato comum de secret real entrou nos arquivos tocados.

### Critério de saída do Marco 0

- [ ] Uma pessoa parte do diretório do projeto e gera o APK seguindo apenas o runbook.
- [x] O app abre em ao menos um aparelho ou emulador.
- [x] A stack local sobe por procedimento documentado.
- [x] API e banco respondem ao smoke test documentado.
- [x] README e CLAUDE refletem o estado real.
- [x] Pendências e bloqueios estão registrados no diário.

## Marco 1 — produto e design

- [x] Escrever escopo e não escopo do MVP.
- [x] Formalizar a jornada prioritária.
- [x] Criar mapa de telas e navegação.
- [ ] 👤 Formalizar/revisar tokens de cor, tipografia, espaçamento, shapes e estados.
- [ ] 👤 Definir/revisar visualmente componentes de status, risco e aprovação.
- [x] Especificar loading, vazio, erro, offline e sessão expirada.
- [ ] 👤 Validar login, workspace, dashboard, tarefa, approval e resultado em tela pequena.
- [ ] 👤 Registrar decisão humana de dark-only ou dark-first em ADR.

## Marco 2 — autenticação OIDC

- [x] Escolher/configurar provedor OIDC de referência local (Keycloak).
- [x] Criar ADR de autenticação e autorização.
- [x] Criar threat model inicial.
- [x] Definir issuer, audience, scopes e redirect URIs por ambiente.
- [x] Adicionar identidade externa `(issuer, subject)` ao modelo de dados.
- [x] Implementar middleware de validação de access token na API.
- [x] Implementar `GET /v1/me`.
- [x] Impedir identidade fixa fora de `APP_ENV=local` + `AUTH_MODE=dev` explícito.
- [ ] Implementar login Android com Authorization Code + PKCE S256.
- [ ] Implementar armazenamento protegido de sessão.
- [ ] Implementar renovação/expiração/logout.
- [ ] Proteger navegação Android.
- [x] Testar assinatura/token inválido, expirado, issuer e audience incorretos.
- [ ] Testar isolamento entre workspaces.
- [ ] Verificar que tokens não aparecem em logs ou URLs.

**Evidência atual:** Keycloak `26.7.1` saudável, cliente público com PKCE `S256`, redirect exato e grants inseguros desabilitados. `automation/scripts/oidc-local-smoke.py` concluiu Authorization Code + PKCE, confirmou `401` sem token e `/v1/me` com usuário/workspace, sem imprimir/persistir token. OIDC autentica; os handlers de recurso ainda não autorizam membership/workspace e não podem ser expostos externamente.

## Marco 3 — integração Android/API

- [x] Escolher e registrar Retrofit ou Ktor Client.
- [x] Implementar cliente HTTP autenticado.
- [x] Implementar tratamento uniforme de erros.
- [x] Substituir fake de workspace/dashboard.
- [x] Substituir fake de tarefas.
- [x] Substituir fake de agents/devices/approvals.
- [x] Autenticar e reconectar WebSocket.
- [x] Manter fakes apenas em previews e testes.
- [x] Adiar Room até existir requisito offline mensurável.

**Evidência atual:** ADR 0003 registra Retrofit `2.12.0`, Gson e OkHttp `4.12.0`. Runtime usa cinco repositórios remotos; fakes existem somente em `src/test`. Bearer é aplicado em header pelo mesmo cliente HTTP/WebSocket, sem token em URL; o store atual é somente em memória e será alimentado pelo login do Marco 2. Erros remotos viram falhas de domínio sanitizadas. WebSocket reconecta com backoff e reconsulta o snapshot REST. `testDebugUnitTest assembleDebug assembleRelease` passou com 13 testes e R8; MockWebServer cobriu token, erros, DTOs, integridade e reconexão. Os cinco contratos também passaram contra a API local. No MC2200, o smoke instrumentado com `adb reverse` passou `4/4` e abriu o detalhe remoto de `#184`. Isso não remove o gate BOLA nem autoriza publicar o Tunnel.

## Marco 4 — cockpit funcional

- [ ] Criar tarefa pela UI.
- [ ] Exibir timeline de execução em tempo real.
- [ ] Exibir agente e máquina responsáveis.
- [ ] Exibir approval com risco e consequência.
- [ ] Aprovar/rejeitar e auditar a decisão.
- [ ] Exibir resultado e artefato.
- [ ] Cobrir o fluxo principal com teste E2E.

## Marco 5 — device agent

- [ ] Registrar dispositivo com identidade própria.
- [ ] Implementar heartbeat.
- [ ] Anunciar capabilities autorizadas.
- [ ] Implementar uma ação allowlisted.
- [ ] Bloquear comandos arbitrários.
- [ ] Produzir eventos e auditoria.
- [ ] Exigir approval para ação sensível de demonstração.

## Marco 6 — beta interna

- [ ] CI Android/Go/Java/migrations.
- [ ] Testes unitários e de integração mínimos.
- [ ] Smoke test automatizado.
- [ ] APK interno assinado.
- [ ] Configuração de homologação isolada.
- [ ] Logs sem secrets e métricas mínimas.
- [ ] Runbook de instalação, rollback e recuperação.
