# Work Control

Work Control é um **cockpit mobile para trabalho assistido por agentes de IA**. A proposta é permitir que um desenvolvedor inicie, acompanhe, aprove e controle trabalhos executados por agentes de IA em computadores, servidores e ambientes remotos sem precisar permanecer fisicamente diante da máquina.

> O produto não é um chatbot. A unidade principal é o **trabalho**: `Workspace → Projeto → Tarefa → Agentes → Execuções → Artefatos/Resultados`.

## Estado atual do MVP

O projeto concluiu a integração local do **Marco 3 — Android/API**. O login
Android e a autorização BOLA do Marco 2 continuam como gates antes de qualquer
exposição externa. Revisões humanas de design e a configuração Git permanecem
deliberadamente pendentes.

- app Android nativo em Kotlin/Compose, com Hilt, navegação, design system, Retrofit/Gson, OkHttp/WebSocket e repositórios remotos para Home/Tarefas/Detalhe;
- API Go com schema PostgreSQL, migrations, seed, REST, WebSocket, simulador de eventos, healthchecks, modos `dev`/`oidc`, validação JWT via Discovery/JWKS e `GET /v1/me`;
- Docker Compose com PostgreSQL, Redis, migration, API, seed e Keycloak local opcional, com portas publicadas somente em loopback;
- testes unitários Android, suíte instrumentada de navegação executada `4/4` em tela pequena e testes Go reais de auth, config, health, WebSocket e criação de tarefa contra PostgreSQL;
- documentação de escopo, jornada, navegação, estados de UI, ADR OIDC, matriz por ambiente e threat model;
- 13 testes Android cobrindo domínio/ViewModels, Bearer, erros, DTOs/repositórios, integridade e reconexão WebSocket;
- protótipo React/Figma preservado como referência visual; fakes Android existem somente em testes.

O Keycloak e a API já passaram por um smoke real de Authorization Code + PKCE,
access token e `/v1/me`. O app já consome REST/WebSocket da API local, mas ainda
não estão implementados o login/sessão protegida no Android, o isolamento de
recursos por membership, o device agent funcional e o orquestrador Java real. O
Compose mantém `AUTH_MODE=dev` explicitamente local; essa ponte falha ao iniciar
fora de `APP_ENV=local`.

O plano, acompanhamento e histórico operacional estão em:

- [`PLANO_DE_ACAO_MVP.md`](PLANO_DE_ACAO_MVP.md);
- [`CHECKLIST_MVP.md`](CHECKLIST_MVP.md);
- [`DIARIO_DE_BORDO.md`](DIARIO_DE_BORDO.md);
- [`docs/runbooks/local-development.md`](docs/runbooks/local-development.md).

Commits estão temporariamente adiados enquanto o mantenedor configura dual Git no computador da empresa. Não executar `git init` nem assumir identidade/remoto até essa configuração ser concluída.

## Quickstart da baseline

Com JDK 21 completo, Android SDK 35, Go 1.25 e Docker Compose v2:

```bash
sdk env
./automation/scripts/verify-local.sh

docker compose -f infra/docker/docker-compose.yml --profile dev up -d --build
curl --fail http://localhost:8080/health/ready
curl --fail http://localhost:8080/v1/me
```

APK de debug:

```text
apps/android/app/build/outputs/apk/debug/app-debug.apk
```

Instalação em um aparelho autorizado:

```bash
adb devices
adb reverse tcp:8080 tcp:8080
adb install -r apps/android/app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.workcontrol.app.debug/com.workcontrol.app.MainActivity
```

Consulte o [runbook local](docs/runbooks/local-development.md) para setup, testes
com PostgreSQL, Keycloak/OIDC, smoke PKCE e troubleshooting do JDK/ADB.

## Objetivo do MVP

Validar se programadores usariam o celular como interface operacional para:

- identificar ou selecionar o workspace atual;
- visualizar tarefas pendentes e execuções em andamento;
- acompanhar vários agentes de IA em tempo real;
- saber em qual máquina cada execução está ocorrendo;
- abrir logs, arquivos, diffs e terminal/SSH;
- enviar arquivos entre celular, projeto, máquina e agente;
- aprovar ou rejeitar operações sensíveis;
- receber o resultado final de uma tarefa sem ficar preso ao desktop.

A localização é apenas um **sinal de contexto** para sugerir o workspace. Nunca deve ser utilizada como autenticação ou autorização.

## Fluxo principal

1. O usuário abre o app e o Work Control sugere um workspace, por exemplo `Escritório / Projeto Atlas`.
2. O dashboard mostra tarefas, agentes ativos, máquinas conectadas e aprovações pendentes.
3. O usuário cria uma tarefa, como `Investigue e corrija o bug #184`.
4. O orquestrador divide o trabalho e seleciona agentes adequados.
5. Um agente de código altera arquivos na Workstation; outro executa testes; um agente DevOps prepara deploy.
6. O app recebe eventos em tempo real e mostra uma timeline da execução.
7. Uma operação sensível, como deploy, pausa e solicita aprovação humana.
8. Após aprovação, a execução continua e o resultado fica associado à tarefa.

## Entidades principais

- **User**: proprietário ou membro de um workspace.
- **Workspace**: contexto de trabalho, por exemplo Casa, Escritório ou Cliente ACME.
- **Project**: projeto de software associado a um workspace.
- **Device**: workstation, notebook, servidor ou agente local conectado.
- **Task**: objetivo solicitado pelo usuário.
- **Agent**: executor especializado, como Dev, QA, DevOps ou Research.
- **Execution**: tentativa/execução concreta de um agente.
- **Approval**: autorização humana para uma ação sensível.
- **Artifact**: arquivo, diff, log, relatório ou saída produzida.
- **Event**: atualização em tempo real de tarefa, agente ou dispositivo.

## Arquitetura proposta

```text
Android App (Kotlin)
        |
 Cloudflare Zero Trust
        |
     NGINX
        |
  API / Gateway (Go) ---------------- Redis
        |                               |
        |                         eventos/cache/locks
        |
 Orchestrator (Java 21)
        |
        +------ PostgreSQL
        |
 Device Gateway / Agent (Go)
        |
   PC / Server / SSH / Docker / Git
```

### Android — Kotlin

Responsável pela experiência mobile:

- autenticação e sessão;
- seleção/detecção de workspace;
- dashboard;
- tarefas e timeline;
- agentes e execuções;
- aprovações biométricas;
- máquinas;
- terminal/SSH;
- arquivos e diffs;
- notificações e eventos em tempo real.

Arquitetura Android adotada: **Clean Architecture + MVVM**, Jetpack Compose,
Coroutines/Flow, Retrofit e WebSocket. Room permanece adiado até existir um
requisito offline mensurável.

No código atual, Retrofit `2.12.0` + Gson consome REST e OkHttp `4.12.0` atende
HTTP/WebSocket com o mesmo interceptor Bearer. Debug usa
`http://localhost:8080/` por `adb reverse`; release assume
`https://work-control.xandehome.api.br/`. URL é configuração, não segredo.
Credenciais de Cloudflare Access nunca são embarcadas no APK. Consulte o
[ADR 0003](docs/adr/0003-android-retrofit-cloudflare-edge.md) e a
[preparação do Tunnel](infra/cloudflare/README.md).

### API e Device Agent — Go

Go é adequado para componentes de rede, concorrência e comunicação com máquinas.

`api-go`:

- REST/gRPC/WebSocket;
- autenticação/autorização;
- workspaces e projetos;
- tarefas;
- dispositivos;
- aprovações;
- artefatos;
- stream de eventos.

`device-agent-go` é instalado nas máquinas autorizadas e anuncia capacidades, por exemplo:

```json
{
  "device": "Workstation Dev",
  "capabilities": ["terminal", "filesystem", "git", "docker", "screen"]
}
```

O agente deve operar com **privilégio mínimo** e capacidades explicitamente autorizadas.

### Orchestrator — Java 21

Serviço responsável pela lógica de workflow:

- decomposição de tarefas;
- seleção de agentes;
- dependências entre etapas;
- máquina de estados;
- políticas de aprovação;
- retries, timeout e cancelamento;
- adapters para diferentes provedores de IA;
- auditoria da execução.

Java 21 permite utilizar virtual threads quando fizer sentido para trabalhos concorrentes/I/O. JUnit 5 será a base dos testes unitários e de integração deste serviço.

## Banco de dados — PostgreSQL

PostgreSQL é a fonte persistente de verdade. Tabelas iniciais sugeridas:

- users
- workspaces
- workspace_members
- projects
- devices
- device_capabilities
- agents
- tasks
- task_steps
- executions
- execution_events
- approvals
- artifacts
- credentials_metadata
- audit_logs
- external_identities

Credenciais e secrets reais não devem ser armazenados diretamente em texto puro no banco.

## Redis

Usar Redis para dados efêmeros:

- cache;
- sessões de curta duração;
- presença/heartbeat de máquinas;
- locks distribuídos;
- rate limiting;
- filas/eventos quando apropriado;
- estado transitório de streams.

PostgreSQL continua sendo a fonte persistente de verdade.

## Segurança

A segurança é parte central do produto.

### Zero Trust

Cloudflare Zero Trust deve proteger endpoints administrativos, ambientes internos e acessos privados. Uma máquina não deve ser considerada confiável apenas por estar na rede correta.

### Permissões por capacidade

Exemplo:

```text
Agent Dev
  ✓ leitura do repositório
  ✓ alteração de código
  ✓ execução de testes
  ✕ deploy em produção
  ✕ leitura de secrets

Agent DevOps
  ✓ Docker
  ✓ staging
  ✓ preparar deploy
  ! produção requer aprovação humana
```

### Operações sensíveis

Ações como deploy, alteração destrutiva, acesso a produção, uso de credenciais ou comandos privilegiados devem gerar `Approval` antes da execução.

### Fail2ban

Fail2ban pode ser utilizado em hosts/bastions expostos, especialmente serviços SSH. Ele não substitui Cloudflare Zero Trust, firewall, autenticação forte ou políticas Kubernetes.

### Auditoria

Toda ação relevante deve produzir evento de auditoria com:

- usuário;
- agente;
- dispositivo;
- comando/ação abstrata;
- timestamp;
- resultado;
- aprovação associada, quando houver.

Nunca registrar secrets em logs.

## Ambientes

### Local

O Docker Compose atual sobe PostgreSQL, Redis, migrations, API Go e seed. O orquestrador Java ainda não está conectado à stack; sua inclusão continua sendo parte da arquitetura-alvo.

### Homologação

Cluster Kubernetes separado ou namespace isolado com:

- dados não produtivos;
- domínio próprio;
- secrets próprios;
- integrações sandbox;
- observabilidade;
- testes de integração e E2E antes de promoção.

### Produção

Produção deve possuir:

- isolamento de homologação;
- secrets independentes;
- backups PostgreSQL;
- políticas de rede;
- limites CPU/memória;
- readiness/liveness probes;
- autoscaling quando necessário;
- logs e métricas centralizados;
- estratégia de rollback.

## Docker e Kubernetes

Cada serviço deve ter Dockerfile próprio. O diretório `infra/k8s/base` contém recursos comuns e `infra/k8s/overlays` mantém customizações de homologação e produção.

Estratégia recomendada: Kustomize inicialmente; Helm pode ser introduzido quando a quantidade de parametrização justificar.

## NGINX

NGINX pode atuar como ingress/reverse proxy para:

- TLS/origin;
- roteamento;
- limites de payload;
- timeouts de WebSocket;
- headers de segurança;
- rate limiting complementar.

Cloudflare permanece na borda pública sempre que aplicável.

## Testes

### Android

- JUnit para ViewModels/use cases/regras de negócio;
- testes instrumentados para fluxos críticos;
- testes de Compose para UI.

### Java 21

- JUnit 5;
- testes unitários do workflow;
- integração com PostgreSQL/Redis usando containers efêmeros quando possível.

### Go

- `go test ./...`;
- testes de handlers;
- testes de autorização;
- testes de protocolo do device-agent.

### E2E

Fluxos mínimos:

1. criar tarefa;
2. conectar dispositivo;
3. iniciar execução;
4. receber eventos;
5. solicitar aprovação;
6. aprovar;
7. concluir tarefa;
8. consultar artefatos e auditoria.

## Automação / CI-CD

Pipeline esperado:

```text
Pull Request
  → lint
  → testes unitários
  → testes de integração
  → build de imagens
  → análise de segurança
  → deploy homologação
  → smoke/E2E
  → aprovação
  → deploy produção
```

Nunca promover automaticamente para produção uma alteração que envolva migrations destrutivas ou políticas críticas sem gate apropriado.

## API inicial sugerida

```text
POST   /v1/tasks
GET    /v1/tasks
GET    /v1/tasks/{id}
POST   /v1/tasks/{id}/cancel

GET    /v1/workspaces
GET    /v1/workspaces/{id}/dashboard

GET    /v1/devices
GET    /v1/devices/{id}
POST   /v1/devices/{id}/commands

GET    /v1/agents
GET    /v1/executions/{id}
GET    /v1/executions/{id}/events

GET    /v1/approvals
POST   /v1/approvals/{id}/approve
POST   /v1/approvals/{id}/reject
```

A API de execução remota precisa de políticas explícitas e não deve aceitar comandos arbitrários de clientes sem validação/autorização.

## Escopo recomendado do primeiro release

Implementar primeiro:

1. autenticação;
2. workspace manual + sugestão por contexto;
3. cadastro e heartbeat de máquinas;
4. criação de tarefas;
5. um único provider de IA por trás de interface abstrata;
6. timeline de execução em tempo real;
7. agente local com capacidades limitadas;
8. leitura de logs/diffs;
9. aprovação humana;
10. terminal/SSH controlado;
11. homologação completa antes de produção.

Evitar no primeiro MVP: marketplace de agentes, dezenas de providers, editor de código completo, remote desktop avançado e automações complexas. O primeiro objetivo é validar o **cockpit de execução**.

## Estrutura do repositório

```text
work-control-mvp/
├── apps/
│   └── android/
│       └── app/src/
│           ├── main/kotlin/com/workcontrol/app/
│           ├── test/kotlin/com/workcontrol/app/
│           └── androidTest/kotlin/com/workcontrol/app/
├── services/
│   ├── api-go/
│   │   ├── cmd/api/
│   │   └── internal/{auth,workspace,tasks,agents,devices,approvals,files,ssh,events}/
│   ├── orchestrator-java/
│   │   └── src/{main,test}/java/com/workcontrol/orchestrator/
│   └── device-agent-go/
│       ├── cmd/agent/
│       └── internal/{capabilities,executor,transport,security}/
├── infra/
│   ├── docker/
│   ├── k8s/
│   │   ├── base/
│   │   └── overlays/{homolog,prod}/
│   ├── nginx/
│   ├── cloudflare/
│   ├── fail2ban/
│   ├── postgres/
│   └── redis/
├── automation/
│   ├── scripts/
│   └── workflows/
├── docs/
│   ├── architecture/
│   ├── security/
│   ├── api/
│   └── adr/
├── tests/
│   ├── integration/
│   ├── e2e/
│   └── load/
└── .github/workflows/
```

## Roadmap sugerido

**Marco 3 local — integração Android/API:** Retrofit, repositórios remotos e
WebSocket autenticável/reconectável estão implementados. As próximas fatias são
login/sessão Android e autorização por workspace antes da publicação Cloudflare.
Pendências humanas/Git do Marco 0/1 permanecem rastreadas no checklist.

**Fase 1 — Fundação:** Android shell, autenticação, workspaces, API, banco e device registration.

**Fase 2 — Cockpit:** tarefas, agentes, timeline em tempo real e dashboard.

**Fase 3 — Execução:** device agent, arquivos, Git, logs e terminal limitado.

**Fase 4 — Segurança:** approvals, RBAC/capabilities, auditoria, Zero Trust e hardening.

**Fase 5 — Operação:** homologação, CI/CD, observabilidade, backups, produção e rollback.

## Regra de produto

Em qualquer tela de execução o usuário deve conseguir responder rapidamente:

**O que está acontecendo? Quem está fazendo? Onde está rodando? Qual foi o resultado? Precisa de mim?**

Essa regra deve orientar tanto o design quanto a arquitetura do sistema.
