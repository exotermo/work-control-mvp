# Runbook — Desenvolvimento local

Este é o caminho canônico para validar a baseline local do Work Control. Os comandos partem da raiz do projeto.

## 1. Pré-requisitos

- Linux, macOS ou Windows com WSL2;
- JDK 21 completo, incluindo `javac` e `jlink`;
- Android SDK com platform/compile SDK 35;
- ADB e um aparelho físico com depuração USB ou um emulador;
- Go 1.25 ou compatível com o `go.mod` da API;
- Docker Engine com Docker Compose v2;
- `curl` para os smoke tests.
- Python 3 apenas para o smoke OIDC local automatizado.

Git não é pré-requisito para executar a baseline atual. A configuração do repositório está deliberadamente adiada até o mantenedor concluir o dual Git no computador da empresa.

## 2. Selecionar o Java

O projeto possui `.sdkmanrc` com `java=21.0.2-tem`. Com SDKMAN instalado:

```bash
sdk env
java -version
jlink --version
```

Se `sdk env` não estiver disponível, selecione manualmente um JDK 21 completo:

```bash
export JAVA_HOME="$HOME/.sdkman/candidates/java/21.0.2-tem"
export PATH="$JAVA_HOME/bin:$PATH"
java -version
jlink --version
```

### Erro conhecido: `jlink executable ... does not exist`

O ambiente pode apontar `JAVA_HOME` para `/usr/lib/jvm/java-21-openjdk`, que nesta máquina contém somente o runtime. O Android Gradle Plugin precisa de um JDK completo para transformar `core-for-system-modules.jar`.

Confirme:

```bash
test -x "$JAVA_HOME/bin/jlink"
```

Se o comando falhar, selecione o JDK SDKMAN indicado acima ou outro JDK 21 completo. Não registre um caminho absoluto da máquina no `gradle.properties`.

## 3. Verificação rápida da baseline

O script abaixo executa testes/build Android, compilação da API Go e validação sintática do Compose:

```bash
./automation/scripts/verify-local.sh
```

Ele não sobe containers, não instala APK e não altera Git.

## 4. Android

### Testar e gerar APK

```bash
cd apps/android
./gradlew testDebugUnitTest assembleDebug
```

APK esperado:

```text
apps/android/app/build/outputs/apk/debug/app-debug.apk
```

O app runtime usa Retrofit. As URLs são configuração não secreta e precisam
terminar com `/`:

| Variante | Chave | Default |
|---|---|---|
| debug | `BASE_URL_OVERRIDE` | `http://localhost:8080/` |
| release | `API_BASE_URL` | `https://work-control.xandehome.api.br/` |

As chaves podem ser definidas no `apps/android/local.properties` (ignorado pelo
Git) ou como variáveis de ambiente. Release nunca lê `BASE_URL_OVERRIDE`.
Não adicionar API key, client secret ou service token Cloudflare a `BuildConfig`:
valores compilados no APK são extraíveis.

### Instalar em aparelho ou emulador

Com o dispositivo desbloqueado e autorizado:

```bash
adb devices
adb reverse tcp:8080 tcp:8080
adb install -r apps/android/app/build/outputs/apk/debug/app-debug.apk
```

O reverse da porta `8080` precisa permanecer ativo enquanto o app debug consome
a API local. Verifique com `adb reverse --list`. O cleartext HTTP é permitido
somente no manifest debug; a variante release recusa cleartext.

Se o Gradle/ADB reiniciar o daemon, os forwards podem desaparecer. Ao observar
`ConnectException` para `localhost:8080` no aparelho, execute novamente
`adb reverse tcp:8080 tcp:8080` e confirme a lista antes de repetir o teste.

Abrir a variante debug:

```bash
adb shell am start \
  -n com.workcontrol.app.debug/com.workcontrol.app.MainActivity
```

Para acompanhar crash de inicialização sem despejar dados desnecessários:

```bash
adb logcat -c
adb logcat AndroidRuntime:E '*:S'
```

Não publicar logs antes de revisar tokens, credenciais, caminhos pessoais e dados do aparelho.

### Teste instrumentado de navegação

Compilar o APK de instrumentação não exige dispositivo:

```bash
cd apps/android
./gradlew :app:assembleDebugAndroidTest
```

Com um aparelho/emulador conectado e autorizado, executar os quatro cenários de
navegação por semântica (sem coordenadas):

```bash
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.workcontrol.app.MainActivityNavigationTest
```

Desde o Marco 3, a suíte navega por conteúdo remoto. Suba e aplique o seed da
stack local e configure `adb reverse tcp:8080 tcp:8080` antes de executá-la.

Se houver mais de um alvo ADB, selecione explicitamente um deles para evitar que
o estado de foreground de outro aparelho contamine o resultado:

```bash
ANDROID_SERIAL='<serial mostrado por adb devices -l>' \
  ./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.workcontrol.app.MainActivityNavigationTest
```

A compilação não substitui a execução. Mesmo quando os quatro cenários passam,
a suíte comprova navegação/semântica e não substitui a revisão visual humana.

## 5. API e infraestrutura local

### Validar a configuração

```bash
docker compose -f infra/docker/docker-compose.yml config --quiet
```

### Subir banco, Redis, migration, API e seed

O profile `dev` habilita o seed idempotente:

```bash
docker compose \
  -f infra/docker/docker-compose.yml \
  --profile dev \
  up -d --build
```

PostgreSQL, Redis e API são publicados somente em `127.0.0.1`; não devem ficar
acessíveis pela rede local. A API do Compose declara `AUTH_MODE=dev`, permitido
apenas junto de `APP_ENV=local`.

Consultar o estado:

```bash
docker compose \
  -f infra/docker/docker-compose.yml \
  --profile dev \
  ps -a
```

### Smoke tests

```bash
curl --fail --silent --show-error http://localhost:8080/health/live
curl --fail --silent --show-error http://localhost:8080/health/ready

curl --fail --silent --show-error http://localhost:8080/v1/workspaces

curl --fail --silent --show-error \
  http://localhost:8080/v1/workspaces/00000000-0000-0000-0000-000000000002/dashboard

curl --fail --silent --show-error http://localhost:8080/v1/tasks
curl --fail --silent --show-error http://localhost:8080/v1/me
```

Validar o handshake do stream WebSocket da tarefa de demonstração:

```bash
cd services/api-go
go run ./cmd/ws-smoke
```

O smoke acima comprova conexão e encerramento limpo. O simulador só publica novos eventos enquanto a tarefa `#184` estiver abaixo de 99% de progresso; por isso, receber uma mensagem não é requisito desse teste de handshake.

No stack Compose, a API injeta usuário/workspace fixos somente porque
`APP_ENV=local` e `AUTH_MODE=dev` estão explícitos. Fora de local essa combinação
falha no startup. O modo OIDC e seu smoke real são descritos na seção 6.
Ao executar a API diretamente no host, `HTTP_HOST` usa o default seguro
`127.0.0.1`; o container sobrescreve explicitamente para `0.0.0.0`, mas sua
publicação continua limitada ao loopback do host.

### Logs operacionais

```bash
docker compose \
  -f infra/docker/docker-compose.yml \
  --profile dev \
  logs --tail=100 api migrate seed
```

Antes de compartilhar logs, revisar qualquer dado sensível.

### Encerrar a stack

```bash
docker compose \
  -f infra/docker/docker-compose.yml \
  --profile dev \
  down
```

O comando acima preserva o volume de dados. A remoção de volumes é destrutiva e não faz parte deste runbook.

## 6. Provedor OIDC local de referência

O profile opcional `oidc` inicia o Keycloak 26.7.1 em modo de desenvolvimento e
importa o realm `work-control-local`. Ele não altera a autenticação atual da API
do Compose e não é iniciado pelo fluxo padrão do profile `dev`.

> **Somente desenvolvimento local:** o modo `start-dev`, o administrador e o
> usuário abaixo são deliberadamente inseguros e conhecidos. Não publique essa
> instância, não reutilize as senhas e não copie essa configuração para produção.

Subir somente o provedor:

```bash
docker compose \
  -f infra/docker/docker-compose.yml \
  --profile oidc \
  up -d keycloak
```

Ou subir a stack local, o seed e o provedor juntos:

```bash
docker compose \
  -f infra/docker/docker-compose.yml \
  --profile dev \
  --profile oidc \
  up -d --build
```

Endpoints e dados locais:

| Item | Valor local |
|---|---|
| Issuer | `http://localhost:8180/realms/work-control-local` |
| Discovery | `http://localhost:8180/realms/work-control-local/.well-known/openid-configuration` |
| Console administrativo | `http://localhost:8180/admin/` |
| Administrador | `local-admin` / `local-only-admin-password` |
| Login de demonstração | `local-developer` / `local-only-password` |
| Subject local fixo | `10000000-0000-0000-0000-000000000001` |
| Cliente Android | `work-control-android-local` (público, sem secret) |
| Scope obrigatório da API | `work-control.api` |
| Audiência da API | `urn:work-control:api:local` |

O cliente Android aceita apenas Authorization Code, exige PKCE `S256` e permite
exatamente o callback debug `com.workcontrol.app.debug:/oauth2redirect`.
`profile`, `email` e `work-control.api` são scopes padrão; `offline_access` é
opcional e só deve ser solicitado quando a gestão
segura de refresh tokens estiver implementada no aplicativo. O scope `openid`
é solicitado no pedido de autorização, conforme o protocolo, e não é cadastrado
como client scope do Keycloak.

Validar que o realm está disponível:

```bash
curl --fail --silent --show-error \
  http://localhost:8180/realms/work-control-local/.well-known/openid-configuration
```

Esta referência usa o banco H2 efêmero do próprio container. `down` ou
`--force-recreate` recriam o estado a partir do JSON; `stop` preserva o estado do
container existente. Depois de alterar o arquivo de realm, aplique-o localmente
com `up -d --force-recreate keycloak`.

### Acesso a partir do Android

`localhost` dentro do Android aponta para o próprio aparelho, não para o
computador. Para um aparelho físico conectado por USB, mantenha o issuer acima e
crie o túnel antes de abrir o login no navegador do sistema:

```bash
adb reverse tcp:8180 tcp:8180
adb reverse --list
```

O reverse também precisa estar ativo durante a troca do authorization code e as
renovações de token. Para remover somente esse túnel:

```bash
adb reverse --remove tcp:8180
```

Emuladores Android normalmente alcançam o host como `10.0.2.2`, mas trocar o
hostname muda o valor do issuer. Não misture `localhost` e `10.0.2.2` numa mesma
sessão OIDC: discovery, autorização e validação do token precisam concordar com
o issuer. Para esta referência, prefira `adb reverse`, que também funciona com
emulador conectado via ADB.

A porta é publicada apenas na interface de loopback do computador. Testes em um
aparelho sem USB/ADB exigem uma estratégia de hostname, TLS e confiança de
certificado que não faz parte desta baseline local.

### Smoke real da API em modo OIDC

O seed local vincula o subject fixo acima ao usuário interno por
`(issuer, subject)`. Como o issuer canônico usa `localhost`, a API OIDC é
executada no host durante este smoke; dentro do container, `localhost` apontaria
para o próprio container. A API do Compose continua disponível em `8080` no modo
dev e a instância temporária OIDC usa `8081`.

Com PostgreSQL, seed e Keycloak ativos, iniciar em um terminal:

```bash
cd services/api-go
APP_ENV=local \
AUTH_MODE=oidc \
HTTP_HOST=127.0.0.1 \
HTTP_PORT=8081 \
POSTGRES_HOST=127.0.0.1 \
OIDC_ALLOWED_ISSUER=http://localhost:8180/realms/work-control-local \
OIDC_AUDIENCE=urn:work-control:api:local \
OIDC_REQUIRED_SCOPE=work-control.api \
OIDC_EXPECTED_TOKEN_TYPE=Bearer \
OIDC_SIGNING_ALGORITHMS=RS256 \
go run ./cmd/api
```

Em outro terminal, a partir da raiz:

```bash
./automation/scripts/oidc-local-smoke.py
```

O utilitário automatiza o formulário do usuário exclusivamente local, cria PKCE
S256, troca o authorization code, confirma `401` sem token e chama `/v1/me` com
o access token. Ele não imprime nem persiste tokens. A saída esperada é:

```text
OIDC local smoke: OK (user=00000000-0000-0000-0000-000000000001, workspaces=1)
```

Esse smoke valida provider e API, mas não substitui o login Android nem comprova
autorização dos demais recursos por workspace. Pare a API temporária com
`Ctrl+C` após o teste.

### Encerrar o provedor

```bash
docker compose \
  -f infra/docker/docker-compose.yml \
  --profile oidc \
  stop keycloak
```

## 7. Verificações isoladas

### Contratos Android/API

`testDebugUnitTest` usa MockWebServer e cobre:

- Bearer no header, nunca na query;
- tradução uniforme de erros HTTP/rede/protocolo;
- DTOs e mapeamento dos cinco repositórios remotos;
- handshake autenticado e reconexão WebSocket.

Para o smoke manual da API consumida pelo app:

```bash
curl --fail --silent --show-error http://localhost:8080/v1/me
curl --fail --silent --show-error http://localhost:8080/v1/tasks
curl --fail --silent --show-error http://localhost:8080/v1/agents
curl --fail --silent --show-error http://localhost:8080/v1/devices
curl --fail --silent --show-error http://localhost:8080/v1/approvals
```

### Cloudflare Tunnel

O exemplo sem credenciais e o gate de publicação estão em
[`infra/cloudflare/README.md`](../../infra/cloudflare/README.md). O hostname
preparado é `work-control.xandehome.api.br`. Não publicar enquanto o isolamento
de workspace/BOLA de REST e WebSocket não estiver implementado e testado.

### API Go

```bash
cd services/api-go
go test ./...
```

O comando executa testes de auth/config/health/WebSocket. O teste de criação de
tarefa usa PostgreSQL real e faz rollback; ele é opt-in para não depender de
container no teste unitário comum:

```bash
TEST_DATABASE_URL='postgres://workcontrol:local-only@127.0.0.1:5432/workcontrol?sslmode=disable' \
  go test -count=1 ./internal/tasks
```

Validar separadamente a entrega determinística do envelope WebSocket:

```bash
go test -count=1 ./internal/events \
  -run TestWSHandlerPublishesEnvelopeAfterSubscription
```

Esse teste WebSocket comprova transporte/recebimento; autorização do socket por
workspace continua pendente.

### Orquestrador Java

O orquestrador continua como placeholder e não possui Gradle Wrapper próprio:

```bash
cd services/orchestrator-java
gradle test
```

Essa validação depende de Gradle instalado no sistema e não integra o caminho canônico do Marco 0.

## 8. Critério de aceite do ambiente

- `./automation/scripts/verify-local.sh` termina com sucesso;
- o APK é gerado;
- o app abre sem crash em pelo menos um dispositivo;
- a stack Compose inicia;
- health, workspaces, dashboard, tasks e `/v1/me` retornam sucesso em modo dev;
- a suíte Go real passa; com `TEST_DATABASE_URL`, a criação transacional também passa;
- quando o profile OIDC for validado, o smoke PKCE termina com `OIDC local smoke: OK`;
- qualquer diferença encontrada é registrada em `DIARIO_DE_BORDO.md` e `CHECKLIST_MVP.md`.
