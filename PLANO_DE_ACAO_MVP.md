# Plano de Ação e Execução — Work Control MVP

**Criado em:** 2026-08-24  
**Status:** em execução  
**Marco atual:** Marco 3 integrado localmente; login Android e autorização BOLA seguem como gates  
**Política temporária de Git:** não criar commits nem inicializar/reconfigurar o repositório até a configuração de dual Git no computador da empresa.

## 1. Objetivo

Construir um MVP validável do Work Control por meio de um **walking skeleton autenticado**:

```text
Login
  → seleção de workspace
  → dashboard com dados reais
  → máquina conectada
  → criação de tarefa
  → eventos em tempo real
  → aprovação humana
  → resultado e auditoria
```

O plano privilegia uma fatia vertical funcional antes da expansão de telas, providers, automações e formas avançadas de acesso remoto.

## 2. Estado de partida verificado

- O Android nativo já possui Gradle Wrapper, Kotlin, Jetpack Compose, Hilt, navegação, design system, Retrofit/OkHttp e testes unitários.
- O APK de debug é gerado com sucesso quando o Gradle usa um JDK completo.
- A API Go já possui migrations PostgreSQL, queries tipadas, handlers REST, WebSocket, seed e simulador de eventos.
- A API possui testes reais de auth/config/health/WebSocket e integração PostgreSQL de criação de tarefa.
- O runtime Android usa repositórios Retrofit para workspace, tarefas, agentes, dispositivos e aprovações; fakes ficaram somente nos testes.
- A API possui modos explícitos `dev` e `oidc`; a identidade fixa só inicia em `APP_ENV=local`.
- O protótipo já estabelece uma hipótese visual: cockpit escuro, destaque violeta, cores semânticas de estado e tipografia monoespaçada para dados técnicos.
- A pasta `.git` está vazia e o diretório não é reconhecido como repositório Git.
- Keycloak local, identidade externa `(issuer, subject)`, middleware JWT e `/v1/me` já foram validados por smoke Authorization Code + PKCE.
- Autorização por membership/workspace e login Android continuam pendentes e bloqueiam exposição externa, inclusive pelo Cloudflare Tunnel preparado no Marco 3.

## 3. Princípios de execução

1. Documentação e código evoluem juntos.
2. Cada incremento termina com evidência verificável e registro no `DIARIO_DE_BORDO.md`.
3. Cada mudança atualiza o `CHECKLIST_MVP.md`.
4. Autenticação e autorização são tratadas separadamente.
5. OAuth 2.0 será usado com OpenID Connect para identidade.
6. O Android é cliente público: Authorization Code + PKCE, navegador externo e nenhum client secret no APK.
7. PostgreSQL é a fonte persistente de verdade; Redis permanece efêmero.
8. Workspace, capabilities e approvals são autorizados no backend, não inferidos apenas de claims do cliente.
9. Secrets e tokens nunca são registrados em logs, URLs, código-fonte ou banco em texto puro.
10. Funcionalidades perigosas, como shell arbitrário e SSH aberto, não entram antes das políticas de capacidade, aprovação e auditoria.

## 4. Marcos

| Marco | Objetivo | Entregáveis principais | Critério de aceite |
|---|---|---|---|
| 0 — Baseline | Tornar o projeto reproduzível | Git pendente de configuração, quickstart, ambiente Java/Android, APK executado, Compose validado, documentação atualizada | Outra pessoa consegue ir do código ao app/API seguindo somente o repositório |
| 1 — Produto e design | Formalizar o MVP e a experiência | jornada principal, mapa de telas, princípios visuais, estados de UI e escopo | Login, workspace, dashboard, tarefa e approval possuem fluxo e estados definidos |
| 2 — OIDC | Remover a identidade fixa | ADR, threat model, provedor configurável, middleware JWT, `/v1/me`, login/logout Android | Usuário autentica no navegador e acessa a API; tokens inválidos recebem `401` |
| 3 — Integração real | Remover mocks do caminho principal | cliente HTTP, repositórios remotos, workspace/dashboard/tasks, WebSocket autenticado | Dashboard e tarefas vêm da API; fakes ficam apenas em previews/testes |
| 4 — Cockpit | Validar a proposta de produto | criar tarefa, timeline, approval, resultado e artefato | Fluxo principal funciona ponta a ponta com o simulador |
| 5 — Device agent | Executar trabalho limitado | registro, heartbeat, capabilities, uma ação allowlisted e auditoria | Uma tarefa controlada executa em dispositivo autorizado e produz resultado auditável |
| 6 — Beta interna | Distribuir com segurança | CI, testes, APK assinado interno, observabilidade mínima e runbooks | Instalação repetível e smoke test verde em ambiente limpo |

## 5. Marco 0 — plano detalhado

### 5.1 Saneamento e rastreabilidade

- Preservar a pasta atual sem executar `git init` até a configuração de dual Git.
- Confirmar qual remoto e identidade Git deverão ser usados.
- Depois da configuração, restaurar o metadata original ou inicializar o repositório de forma consciente.
- Registrar a baseline existente antes do primeiro incremento funcional.

### 5.2 Ambiente Android

- Adotar JDK 21 completo para Gradle e para o orquestrador Java.
- Registrar a versão esperada em configuração local do projeto.
- Documentar build, testes, localização do APK e troubleshooting de `JAVA_HOME`/`jlink`.
- Executar o APK em emulador ou aparelho físico.
- Navegar pelas rotas e registrar telas completas, placeholders e problemas visuais.

### 5.3 Backend local

- Validar build da API Go.
- Validar `docker compose config`.
- Subir PostgreSQL, Redis, migrations, API e seed de desenvolvimento.
- Criar/verificar um endpoint de healthcheck quando a implementação de código for iniciada.
- Exercitar pelo menos workspace, dashboard, tasks e WebSocket.

### 5.4 Documentação mínima

- Atualizar `README.md` com quickstart e estado real.
- Atualizar `CLAUDE.md` para que agentes não tratem o projeto como um esqueleto vazio.
- Criar runbook de desenvolvimento local.
- Manter checklist e diário de bordo como fontes de acompanhamento até o Git estar configurado.

## 6. Marco 1 — contrato de produto e design

### Jornada prioritária

```text
Deslogado
  → Entrar
  → autorizar no provedor
  → escolher workspace
  → ver dashboard
  → abrir/criar tarefa
  → acompanhar execução
  → decidir approval
  → consultar resultado
```

### Telas prioritárias

1. Login e falha de autenticação.
2. Seleção de workspace.
3. Dashboard.
4. Lista e criação de tarefas.
5. Detalhe/timeline da tarefa.
6. Aprovação.
7. Resultado.

Cada tela deverá especificar: conteúdo normal, carregamento, vazio, erro, offline, sessão expirada e ação de recuperação.

### Direção visual inicial

- Dark-first com alto contraste e densidade operacional controlada.
- Violeta para navegação e ação primária.
- Verde, amarelo, vermelho, azul e cinza reservados a estados semânticos.
- Tipografia mono apenas para código, máquina, métricas, logs e timestamps.
- Aprovações pendentes sempre visíveis e com linguagem explícita sobre risco e consequência.
- Alvos de toque e texto devem continuar utilizáveis em telas pequenas e com acessibilidade ampliada.

## 7. Marco 2 — arquitetura OIDC

### Fluxo

```text
Android
  → navegador do sistema
  → provedor OpenID Connect
  → callback via redirect URI
  → troca do authorization code com PKCE S256
  → access token para a API
  → ID token para a sessão de identidade do cliente
```

### Android

- Usar AppAuth ou SDK oficial compatível com OIDC e PKCE.
- Usar cliente público, sem segredo estático.
- Usar redirect customizado apenas em debug/local e App Link HTTPS verificado quando houver domínio de homologação/produção.
- Guardar estado/tokens em armazenamento protegido pelo Android Keystore.
- Proteger rotas por estado de sessão.
- Implementar login, renovação, expiração, cancelamento e logout.

### API Go

- Receber o access token no header `Authorization`.
- Descobrir issuer e chaves por OIDC Discovery/JWKS.
- Validar assinatura, algoritmo, `iss`, `aud`, `exp` e demais restrições aplicáveis.
- Mapear identidade por `(issuer, subject)`; e-mail não será chave de segurança.
- Resolver workspace e papel pela associação persistida no banco.
- Diferenciar `401 Unauthorized` de `403 Forbidden`.
- Não aceitar token em query string, inclusive no WebSocket.

### Primeiros incrementos de código

1. `docs(auth)`: ADR, threat model e critérios de aceite.
2. `feat(api)`: validação de access token e `GET /v1/me`.
3. `feat(db)`: identidades externas por issuer/subject.
4. `feat(android)`: login OIDC e gestão de sessão.
5. `feat(android)`: navegação protegida e consumo de `/v1/me`.
6. `test(auth)`: tokens inválidos, expirados, audience incorreta e isolamento de workspace.

As descrições acima são apenas a sequência planejada; os commits permanecerão adiados até o dual Git estar configurado.

## 8. Definição de pronto do MVP

O MVP estará validável quando demonstrar:

- login e logout reais;
- seleção de workspace autorizada;
- máquina registrada com heartbeat e capabilities;
- criação de tarefa;
- pelo menos uma execução controlada;
- eventos em tempo real;
- uma operação bloqueada por aprovação humana;
- decisão de aprovação auditada;
- resultado/artefato associado à tarefa;
- ausência de secrets/tokens em logs;
- instalação e execução reproduzíveis.

## 9. Fora do primeiro corte

- marketplace de agentes;
- múltiplos providers de IA simultâneos;
- editor de código completo;
- remote desktop avançado;
- terminal ou SSH arbitrário;
- upload e sincronização complexa de arquivos;
- automações avançadas;
- cache offline com Room sem requisito comprovado.

## 10. Referências de segurança

- [OpenID Connect Core 1.0](https://openid.net/specs/openid-connect-core-1_0-18.html)
- [RFC 8252 — OAuth 2.0 for Native Apps](https://www.rfc-editor.org/info/rfc8252/)
- [RFC 9700 — Best Current Practice for OAuth 2.0 Security](https://www.rfc-editor.org/rfc/rfc9700.html)
- [RFC 9068 — JWT Profile for OAuth 2.0 Access Tokens](https://www.rfc-editor.org/info/rfc9068/)
- [AppAuth for Android](https://github.com/openid/appauth-android)
