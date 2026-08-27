# Diário de Bordo — Work Control MVP

Registro cronológico das implementações, validações, decisões e bloqueios. Este arquivo será atualizado após cada incremento enquanto os commits estiverem adiados pela configuração de dual Git.

## 2026-08-24 — Entrada 001 — Diagnóstico da baseline

### Realizado

- Leitura integral do `README.md` e do `CLAUDE.md`.
- Inventário da árvore real do projeto.
- Identificação de implementação Android e API Go posterior ao estado descrito no `CLAUDE.md`.
- Identificação de `.git` vazio; o diretório não é reconhecido como repositório.
- Identificação de Android ligado a repositórios fake.
- Identificação de autenticação da API baseada em identidade fixa de desenvolvimento.

### Evidências

- Android nativo presente em `apps/android`.
- API, migrations e seed presentes em `services/api-go`.
- Protótipo React/Figma presente em `Work Control Mobile Prototype`.
- Planos anteriores registrados em `automation/commits`, ainda sem commits aplicados neste diretório.

### Decisão

Tratar o projeto como uma baseline funcional parcial, não como um esqueleto vazio. Não executar `git init` nem criar commits antes da configuração de dual Git.

## 2026-08-24 — Entrada 002 — Validação Android

### Realizado

- Tentativa inicial de build com o `JAVA_HOME` do ambiente.
- Diagnóstico de falha por JRE 21 incompleto, sem `bin/jlink`.
- Localização de JDK 21 completo instalado pelo SDKMAN.
- Execução dos testes unitários e geração do APK de debug com o JDK completo.

### Comando validado

```bash
cd apps/android
JAVA_HOME="$HOME/.sdkman/candidates/java/21.0.2-tem" \
  ./gradlew testDebugUnitTest assembleDebug
```

### Resultado

- `BUILD SUCCESSFUL`.
- APK disponível em `apps/android/app/build/outputs/apk/debug/app-debug.apk`.
- Instalação e inspeção visual ainda pendentes.

### Observação

O caminho absoluto usado na validação pertence ao ambiente atual. O runbook deverá oferecer uma configuração reproduzível sem hardcode de diretório pessoal.

## 2026-08-24 — Entrada 003 — Validação da API Go

### Realizado

- Execução de `go test ./...` em `services/api-go`.

### Resultado

- Exit code `0`.
- Todos os pacotes compilam.
- Todos os pacotes reportam `[no test files]`; portanto, a evidência atual garante compilação, não comportamento.

### Próxima ação

Validar Docker Compose e os endpoints contra PostgreSQL real. Testes de handlers, autorização e store entrarão nos marcos seguintes.

## 2026-08-24 — Entrada 004 — Plano e acompanhamento

### Realizado

- Criação de `PLANO_DE_ACAO_MVP.md`.
- Criação de `CHECKLIST_MVP.md`.
- Criação deste diário.
- Preparação de `ARTIGO_LINKEDIN_WORK_CONTROL.md`.

### Decisão

O primeiro incremento funcional será um walking skeleton autenticado. Antes dele, o Marco 0 consolidará ambiente, execução local, documentação e inspeção do APK.

## 2026-08-24 — Entrada 005 — Ambiente reproduzível

### Implementado

- `.sdkmanrc` na raiz selecionando o JDK `21.0.2-tem`.
- `docs/runbooks/local-development.md` com pré-requisitos, build, testes, APK, ADB, Compose, smoke tests e troubleshooting de `jlink`.
- `automation/scripts/verify-local.sh` para validar Android, API Go e sintaxe do Compose em um único comando.

### Evidência

`./automation/scripts/verify-local.sh` terminou com sucesso, incluindo `testDebugUnitTest`, `assembleDebug`, `go test ./...` e `docker compose config --quiet`.

## 2026-08-24 — Entrada 006 — Stack local e smoke tests

### Realizado

- Build das imagens locais da API e do seed.
- PostgreSQL e Redis iniciados.
- Migration concluída com código 0.
- Seed idempotente concluído com código 0.
- API iniciada na porta 8080.
- Smoke tests executados para workspaces, dashboard e tarefas.

### Resultado

- PostgreSQL saudável.
- API retornando o workspace `Escritório` e tarefas semeadas, incluindo a `#184`.
- A criação de tarefa ainda precisa de smoke próprio.

## 2026-08-24 — Entrada 007 — Smoke WebSocket

### Implementado

- Cliente operacional `services/api-go/cmd/ws-smoke`.
- Instrução correspondente no runbook local.

### Resultado

- Handshake e encerramento limpo do WebSocket concluídos com sucesso.
- O teste não exige mensagem porque o simulador deixa de publicar quando a tarefa `#184` atinge 99%. Uma fixture reiniciável será necessária para validar recebimento de evento de forma determinística.

## 2026-08-24 — Entrada 008 — APK em aparelho físico

### Realizado

- ADB detectou o aparelho inicialmente sem autorização.
- Após autorização manual pelo mantenedor, o APK foi instalado com sucesso.
- Inicialização fria da `MainActivity` terminou com `Status: ok`.
- Processo permaneceu ativo e o buffer de crashes estava vazio.
- Home inspecionada; a linguagem visual dark/violeta e a hierarquia operacional são coerentes como hipótese inicial.

### Problema encontrado

Com navegação Android de três botões, a bottom bar do app ocupava a área do sistema. Seus alvos clicáveis iam de `y=2199` até `2334`, enquanto a barra do sistema começava em `y=2205`. Um toque destinado ao app acionava “Aplicativos recentes”.

Uma captura temporária dessa transição poderia conter conteúdo pessoal de outros aplicativos. Ela não foi analisada nem preservada: todas as capturas e árvores temporárias correspondentes foram apagadas do computador e do aparelho.

### Correção implementada

- Adicionado `navigationBarsPadding()` ao `BottomNavBar`.
- Build e testes Android executados novamente com sucesso.
- APK atualizado no aparelho.
- Novos bounds dos destinos: `y=2064` até `2199`, inteiramente acima da barra do sistema iniciada em `y=2205`.
- Navegação Home → Tarefas → detalhe da tarefa `#184` validada pela árvore de acessibilidade, sem novas capturas de tela.

### Pendências visuais

- Validar placeholders restantes.
- Capturar material de divulgação somente em emulador limpo ou com barra de status recortada.
- Avaliar tipografia, escala e contraste em mais de um tamanho/configuração de fonte.

## 2026-08-24 — Entrada 009 — Healthchecks da API

### Implementado

- `GET /health/live` público para liveness do processo.
- `GET /health/ready` público, validando PostgreSQL com timeout.
- Resposta de readiness indisponível não expõe detalhes internos do banco.
- Quatro testes automatizados em `internal/health`.
- Healthcheck da API no Docker Compose.

### Evidência

- `go test ./...` passou e `internal/health` reportou `ok`.
- Container da API reportou `healthy`.
- Liveness retornou `{"status":"ok"}`.
- Readiness retornou `{"status":"ready"}`.

### Observação operacional

`docker compose up --wait` não deve ser usado com o profile `dev` como critério único, pois o seed é um serviço one-shot e encerra normalmente com código 0. O runbook usa `up -d --build` seguido de `ps -a` e smoke tests explícitos.

## 2026-08-24 — Entrada 010 — Documentação alinhada

### Implementado

- `README.md` atualizado com estado atual, quickstart, arquivos de acompanhamento e diferença entre stack atual e arquitetura-alvo.
- `CLAUDE.md` atualizado para descrever Android/API existentes, comandos reais, JDK, Compose e restrição temporária de Git.
- Artigo do LinkedIn atualizado com o aprendizado da primeira execução física.

### Estado do Git

Nenhum commit foi criado e `git init` não foi executado. A baseline continua aguardando a configuração de dual Git pelo mantenedor.

## 2026-08-24 — Entrada 011 — Verificação final desta rodada

### Realizado

- Execução final de `./automation/scripts/verify-local.sh`.
- Android: build e testes verdes.
- API Go: compilação verde e testes de health verdes.
- Docker Compose: configuração válida.
- Varredura dos arquivos tocados por formatos comuns de chaves privadas, tokens e credenciais.
- Confirmação da remoção dos artefatos temporários de inspeção.

### Resultado

- Verificador terminou com exit code 0.
- Nenhum formato comum de secret real foi encontrado.
- Marco 0 permanece em andamento somente nas pendências explicitamente abertas no checklist, principalmente Git/dual Git, validação independente do runbook, criação de tarefa e evento WebSocket determinístico.

## 2026-08-24 — Entrada 012 — Execução multiagente do checklist

### Orquestração

- Backend: testes reais e fundação OIDC da API.
- Android: smoke instrumentado de navegação.
- Produto/segurança: escopo, jornada, mapa, estados, ADR, matriz e threat model.
- Infra: Keycloak local reproduzível com PKCE.
- Auditoria independente: revisão somente leitura de segurança, runtime e evidências.

### Decisão

Design visual, telas pequenas e a decisão `dark-only`/`dark-first` permanecem na
fila humana. Nenhum commit, `git init` ou ajuste de identidade/remoto foi feito.

## 2026-08-24 — Entrada 013 — Testes reais da API e Android

### Implementado

- Teste WebSocket determinístico: assina o hub, publica um envelope e valida recebimento/encerramento.
- Teste de criação de tarefa contra PostgreSQL real, isolado em transação com rollback e verificação de auditoria.
- Quatro testes instrumentados Compose para Home, Tarefas/detalhe, placeholders e quick action.

### Evidência

- `TEST_DATABASE_URL=... go test -count=1 ./...` passou.
- `go vet ./...` passou.
- `go test -race -count=1 ./internal/events ./internal/auth` passou fora do sandbox, que precisa permitir portas efêmeras do `httptest`.
- `testDebugUnitTest`, `assembleDebug` e `assembleDebugAndroidTest` passaram.
- O aparelho não estava conectado; `connectedDebugAndroidTest` e revisão visual permanecem pendentes.

## 2026-08-24 — Entrada 014 — Contrato de produto e segurança

### Criado

- `docs/product/mvp-scope.md`.
- `docs/product/prioritized-user-journey.md`.
- `docs/product/screen-navigation-map.md`.
- `docs/product/ui-state-contract.md`.
- `docs/adr/0002-provider-neutral-oidc.md`.
- `docs/security/oidc-environment-matrix.md`.
- `docs/security/threat-model-initial.md`.

### Resultado

Escopo, jornada, mapa e estados de UI estão documentados. Tokens visuais,
componentes finais, telas pequenas, IdP de produção e direção dark seguem como
decisões humanas explícitas.

## 2026-08-24 — Entrada 015 — Keycloak local de referência

### Implementado

- Keycloak `26.7.1` no profile opcional `oidc`, publicado apenas em loopback.
- Realm importável com cliente Android público, Authorization Code, PKCE `S256`, redirect debug exato e audience/scope próprios da API.
- Implicit, Direct Grant e Service Account desabilitados no cliente.
- Usuário local com subject fixo e credenciais inequivocamente descartáveis.

### Evidência

- Container `healthy`, discovery/issuer coerentes e configuração efetiva conferida.
- Pedido sem PKCE rejeitado pelo Keycloak.
- O primeiro token real revelou ausência de `sub`; foi adicionado o mapper de subject ao scope `work-control.api` e o realm foi recriado.

## 2026-08-24 — Entrada 016 — Fundação OIDC da API

### Implementado

- `AUTH_MODE=dev|oidc`, com modo dev proibido fora de `APP_ENV=local`.
- Discovery/JWKS e verificação criptográfica via `go-oidc`.
- Validação de algoritmo permitido, assinatura, issuer, audience, expiração, `nbf`, subject, scope e `typ=Bearer`.
- Migration `000002_external_identities`, chave `(issuer, subject)` e vínculo local idempotente pelo seed.
- Resolução por issuer/subject, nunca por e-mail.
- `GET /v1/me` com perfil interno e memberships.

### Limites registrados

- `iat`, `azp/client_id`, TTL/telemetria do cache JWKS e skew configurável são gates antes de homologação.
- Autenticação não resolveu autorização: handlers de recursos ainda precisam restringir toda query ao membership/workspace.
- Mutação/auditoria atômica e geração concorrente de código de tarefa continuam pendentes.

## 2026-08-24 — Entrada 017 — Smoke OIDC ponta a ponta local

### Implementado

- `automation/scripts/oidc-local-smoke.py`, restrito a issuer de loopback.
- O utilitário automatiza Authorization Code + PKCE do usuário local, sem imprimir ou persistir tokens.
- Confirma `401` em `/v1/me` sem Bearer e sucesso com access token real.

### Evidência

```text
OIDC local smoke: OK (user=00000000-0000-0000-0000-000000000001, workspaces=1)
```

A API OIDC temporária rodou no host em `127.0.0.1:8081`, pois o issuer canônico usa
`localhost`; ela foi encerrada após o smoke. A API Compose continua em modo dev
explícito na porta `8080`.

## 2026-08-24 — Entrada 018 — Hardening local e auditoria final

### Implementado

- API, PostgreSQL, Redis e Keycloak publicados apenas em `127.0.0.1`.
- Containers de API/PostgreSQL/Redis recriados sem renovar/remover o volume PostgreSQL.
- Migration preservada em versão `2`, `dirty=false`; cinco tarefas e o vínculo externo permaneceram no banco.
- Runbook, README, CLAUDE, plano e checklist alinhados ao estado executável.

### Resultado da auditoria

Não houve achado bloqueante na verificação JWT, migration/sqlc ou PKCE. O
bloqueio de segurança para exposição externa continua sendo o isolamento de
workspace/BOLA. O checklist não marca essa autorização como concluída e não usa
o teste WebSocket de transporte como evidência de autorização.

## 2026-08-24 — Entrada 019 — Instrumentação em tela pequena e bind da API

### Android

- Dois aparelhos foram encontrados pelo ADB, sem capturas de tela.
- No Samsung, o harness trouxe outra Activity ao foreground e os testes perderam a hierarquia Compose; não houve tentativa de desbloqueio ou inspeção visual.
- No MC2200, a primeira execução revelou duas premissas ruins do teste: conteúdo abaixo da dobra e toque em item ainda fora da área visível.
- Os seletores passaram a usar scroll/ação semântica e validar o topo do detalhe, visível em tela pequena.
- Resultado final no MC2200: `4/4`, zero skips e zero falhas.

### API

- A auditoria identificou que a API OIDC temporária usava `:8081` e podia escutar em todas as interfaces.
- Adicionado `HTTP_HOST`; o runbook agora usa `127.0.0.1:8081` no smoke host.
- O Compose mantém `HTTP_HOST=0.0.0.0` somente dentro do container, cuja publicação externa já está restrita a `127.0.0.1`.

### Decisão

A suíte automatizada confirma navegação e acessibilidade sem coordenadas. A
avaliação estética dos placeholders/tela pequena continua reservada ao mantenedor.

## 2026-08-24 — Entrada 020 — Verificação consolidada da rodada

### Evidências finais

- `./automation/scripts/verify-local.sh`: sucesso para unit tests/APKs Android, testes Go e Compose com/sem profile OIDC.
- `TEST_DATABASE_URL=... go test -count=1 ./...`: sucesso, incluindo criação transacional contra PostgreSQL.
- `go vet ./...`: sucesso.
- `connectedDebugAndroidTest`: `4/4` no MC2200.
- Socket temporário OIDC confirmado exclusivamente em `127.0.0.1:8081`.
- Smoke OIDC real repetido com sucesso após o hardening do bind.
- API Compose reconstruída e `healthy`; API, Keycloak, PostgreSQL e Redis publicados somente em loopback.
- Varredura final sem padrões comuns de private key, token GitHub, AWS access key ou API key `sk-`.

### Estado

Nenhum commit ou comando Git foi executado. A próxima frente técnica segura é
autorização por membership/workspace; login Android pode avançar em paralelo com
as revisões humanas de design, sem marcar isolamento como concluído.

## 2026-08-24 — Entrada 021 — Marco 3, Retrofit e borda Cloudflare

### Android/API

- Registrado o ADR 0003: Retrofit `2.12.0`, Gson e OkHttp `4.12.0` sem atualizar
  implicitamente o toolchain Kotlin do projeto.
- `API_BASE_URL` de release usa por default
  `https://work-control.xandehome.api.br/`; debug usa
  `http://localhost:8080/` e aceita `BASE_URL_OVERRIDE`.
- Cleartext foi negado no manifest principal e liberado apenas na variante debug
  para o fluxo local com `adb reverse`.
- Implementados DTOs Retrofit e cinco repositórios remotos: workspace, tarefas,
  agentes, dispositivos e aprovações.
- Os fakes foram removidos de `src/main` e mantidos somente em `src/test`.
- O dashboard deixou de usar o rótulo de workspace fixo e resolve o workspace
  corrente por `/v1/me`.
- Implementado interceptor Bearer comum a HTTP e WebSocket. O token fica somente
  em memória até o login/Keystore do Marco 2; nenhum segredo Cloudflare foi
  adicionado ao app.
- Implementada tradução sanitizada de `401`, `403`, `404`, `409`, `429`, falha
  de rede, `5xx` e protocolo.
- Home, lista e detalhe agora têm loading, vazio, erro e retry, preservando dados
  anteriores durante falha de atualização.
- WebSocket reconecta com backoff limitado; cada evento invalida e recarrega o
  snapshot REST, que permanece a fonte de verdade.

### Falha encontrada pelos testes

A primeira implementação tentou mudar um `HttpUrl` para scheme `ws/wss`. OkHttp
aceita `HttpUrl` apenas `http/https` e faz o upgrade internamente no
`newWebSocket`; os dois testes de socket falharam antes do handshake. O cliente
foi corrigido para manter HTTP(S), preservando WSS quando a origem é HTTPS.

### Cloudflare

- Preparado `infra/cloudflare/tunnel-config.example.yml` com hostname
  `work-control.xandehome.api.br`, origem em loopback e catch-all `404`.
- Documentado que service tokens são credenciais máquina-a-máquina e nunca podem
  entrar no APK.
- A publicação DNS/Tunnel não foi executada: faltam UUID/conta operacional e,
  principalmente, autorização BOLA em REST/WebSocket.
- Para beta interna, foi registrada a opção WARP + rota privada; para hostname
  mobile publicado, OIDC da API continua obrigatório.

### Evidência

- `testDebugUnitTest assembleDebug assembleRelease`: `BUILD SUCCESSFUL`, 13
  testes e minificação R8 concluída.
- MockWebServer comprovou Bearer sem query, classificação de erro, contratos dos
  repositórios, handshake autenticado e reconexão após `503`.
- Stack Docker permaneceu saudável em loopback.
- `/v1/me`, `/v1/tasks`, `/v1/agents`, `/v1/devices` e `/v1/approvals` retornaram
  JSON válido no smoke da API real local.
- APK debug instalado no MC2200 e Activity iniciada a frio com sucesso.
- Smoke instrumentado final no MC2200: `4/4`, consumindo a API real por Retrofit
  e abrindo o detalhe remoto de `#184`.

### Ajustes revelados pelo aparelho

- A ordem real do PostgreSQL deixou `#184` abaixo da dobra; o teste foi corrigido
  para rolar a `LazyColumn` por semantics/test tag, sem coordenadas.
- Uma execução intermediária reiniciou o daemon ADB e perdeu `adb reverse`,
  produzindo `ConnectException` nas cinco chamadas. Após reaplicar o forwarding,
  os quatro testes passaram. O runbook agora registra esse diagnóstico.

### Pendências preservadas

Login Android, armazenamento cifrado/renovação/logout, navegação protegida e
isolamento por workspace continuam no Marco 2. O Marco 3 está implementado para
o ambiente local, mas isso não autoriza homologação nem exposição externa.
Design permanece para revisão humana e nenhum comando Git foi executado.
