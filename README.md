# Work Control

Aplicativo Android do **Prelo Control**. O Prelo é a única fonte de identidade, permissões, projetos, tarefas, execuções, aprovações, servidores e deploys. Este repositório contém o cliente móvel; o servidor está em `~/projects/hermes`.

## O que o app oferece

- Login por e-mail, senha e TOTP (ou código de recuperação no login). Se o servidor exigir configuração do TOTP, ela é feita no navegador.
- Sessão móvel com access token somente em memória e refresh rotativo cifrado com chave do Android Keystore. O desbloqueio local usa biometria forte ou credencial do aparelho.
- Contexto de usuário e projetos recebido de `GET /api/v1/me`. A seleção de projeto é apenas preferência de interface; o Prelo valida cada operação, inclusive `X-Project-Id`.
- Home, tarefas, criação e execução, detalhes e turnos, aprovações com step-up TOTP quando o Prelo exigir, servidores, pipeline, arquivos listados e deploys por projeto.
- Atualização por SSE em primeiro plano e polling de reserva. Push FCM opcional quando o Firebase estiver configurado.
- Lista e revogação de aparelhos conectados.

O app não executa deploy e não hospeda backend próprio. Terminal, diff de código e métricas CPU/RAM não aparecem porque não têm recurso equivalente no Prelo.

## Arquitetura

```text
Android UI → cliente HTTP autenticado → Prelo Control
             ├─ /me, /tasks, /approvals, /servers, /pipeline, /projects/*
             ├─ /events/stream (SSE, somente primeiro plano)
             └─ /me/push-token (FCM opcional)

GitHub/Bastion → ActionRequest Prelo → decisão → Bastion executa → resultado Prelo → Work Control lê
```

O servidor decide acesso. `403` mostra **Sem acesso** e não é repetido. Eventos e notificações contêm somente ids e indicam que a tela deve buscar o estado atual via API. A perda do SSE ativa polling; push pode estar desligado no servidor sem impedir o uso do app.

## Desenvolvimento Android

Requisitos: JDK 17 e Android SDK (API 35). Copie `apps/android/local.properties.example` para `apps/android/local.properties` e ajuste `sdk.dir`. Em debug, `PRELO_BASE_URL_OVERRIDE` aponta por padrão para `http://localhost:8082/`. Em aparelho físico, use `adb reverse tcp:8082 tcp:8082` ou configure uma URL HTTPS acessível. A variante release aceita somente HTTPS; configure `PRELO_BASE_URL` no ambiente de build.

```bash
cd apps/android
./gradlew lint testDebugUnitTest assembleDebug
```

O app compila sem `apps/android/app/google-services.json`; nesse caso o push fica inativo. Para habilitar FCM, adicione ao Firebase os application IDs `com.workcontrol.app` e `com.workcontrol.app.debug`, baixe `google-services.json` e coloque o arquivo em `apps/android/app/`. Ele está no `.gitignore`. O servidor Prelo precisa de credencial Firebase própria, configurada fora deste repositório. O canal Android usado é `prelo_alerts`.

Nenhum token ou chave deve entrar no APK, em `BuildConfig`, em logs ou em arquivos de configuração versionados. O único valor de conexão no `BuildConfig` é a URL do Prelo.

## Contratos e estado

- Contratos de autoridade: `~/projects/hermes/docs/CONTRATOS.md`.
- Sessão móvel: `~/projects/hermes/docs/integracoes/sessao-mobile.md`.
- SSE e push: `~/projects/hermes/docs/integracoes/eventos-tempo-real.md` e `push.md`.
- Deploys: `~/projects/hermes/docs/integracoes/action-requests.md`.
- Mapa de telas: [`docs/prelo-integration-map.md`](docs/prelo-integration-map.md).

O Firebase do servidor está inicialmente desligado; o app exibe essa condição em vez de assumir que notificações serão entregues. A ligação com BastionDeploy é feita pelo Prelo, sem autoridade de deploy no Android.
