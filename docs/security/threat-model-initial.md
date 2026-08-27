# Threat model inicial — identidade e cockpit do MVP

**Status:** primeira versão para revisão antes da implementação OIDC  
**Método:** análise de ativos, fronteiras e cenários inspirada em STRIDE  
**Escopo:** Android, OIDC, API, WebSocket, autorização de workspace, tarefas e approvals

## Objetivos de segurança

1. Somente uma identidade autenticada e vinculada acessa um workspace.
2. Uma identidade não lê nem altera recurso de outro workspace por manipular IDs.
3. Código, token ou callback interceptado não produz uma sessão útil ao atacante.
4. Uma ação sensível não ocorre sem decisão humana válida, específica e auditável.
5. Eventos transitórios, estado offline ou retry não criam efeitos duplicados.
6. Secrets, tokens, dados de usuário e artefatos não vazam por logs, URLs ou armazenamento inseguro.
7. Agente/dispositivo executa apenas capabilities explicitamente permitidas.

## Ativos

- conta e sessão do usuário;
- authorization codes, access/refresh/ID tokens e chaves públicas/configuração do issuer;
- memberships, papéis e seleção de workspace;
- código-fonte, diffs, logs, artefatos e metadados de projeto;
- tarefas, execuções, comandos abstratos e resultados;
- approvals e trilha de auditoria;
- identidade/capabilities de agente e dispositivo;
- disponibilidade do cockpit e integridade do stream de eventos.

## Fronteiras de confiança

```text
Pessoa
  | SO Android / app Work Control
  | navegador externo
  | rede não confiável
  | IdP / Authorization Server
  | borda/reverse proxy
  | API Go / Resource Server
  | PostgreSQL (verdade persistente) e Redis (efêmero)
  | orquestrador
  | device agent / host alvo
```

Nenhuma travessia herda confiança da anterior. Em especial, estar na rede corporativa, conhecer um UUID ou receber um evento WebSocket não autoriza a ação.

## Premissas

- O dispositivo Android não está sob controle total do servidor e pode estar perdido, comprometido ou com outras aplicações maliciosas.
- TLS é obrigatório fora da exceção loopback local.
- O IdP autentica usuários; o Work Control mantém autorização de domínio.
- PostgreSQL é a fonte persistente. Redis e WebSocket podem perder/repetir mensagens.
- Device identity e comunicação do agente serão modeladas separadamente; token humano não autentica o agente.

## Cenários e controles

| ID | Ameaça / cenário | Impacto | Controles requeridos | Evidência/teste | Risco residual |
|---|---|---|---|---|---|
| TM-01 | App malicioso intercepta authorization code pelo redirect | Sequestro de sessão | PKCE `S256` por transação; App Link HTTPS verificado em homolog/prod; scheme privado só local | Callback sem verifier correto falha; verificar App Link no aparelho | Médio local; baixo com App Link |
| TM-02 | Login em WebView falsa captura credencial/MFA | Comprometimento de conta | Navegador externo/Custom Tab; WebView proibida | Teste instrumentado confirma intent externo | Baixo, depende do SO/navegador |
| TM-03 | CSRF/code injection ou callback de outra tentativa | Sessão vinculada ao atacante | `state`, `nonce` e PKCE únicos, vínculo transacional e uso único | Callback com state/nonce divergente falha | Baixo |
| TM-04 | Mix-up entre authorization servers | Code/token enviado ao endpoint do atacante | Issuer fechado por build; validar metadata e `iss` da resposta quando suportado; issuer único | Trocar issuer/endpoint em fixture deve falhar | Baixo |
| TM-05 | Redirect amplo/open redirect exfiltra code | Sequestro de fluxo | Redirect exato, sem wildcard; nenhum open redirect; HTTPS fora de local | Cadastro/requests com variação falham | Baixo |
| TM-06 | Secret embutido no APK é extraído | Impersonação do client | Client público sem secret; PKCE; secret scanning do APK/repo | Inspeção do APK não encontra segredo OIDC | Baixo |
| TM-07 | Token vaza em URL, log, analytics, clipboard, backup ou screenshot | Acesso indevido e privacidade | Bearer somente em header; redaction; armazenamento cifrado/sem backup; não usar clipboard; avaliar `FLAG_SECURE` em telas sensíveis | Varredura de logs/telemetria e backup; WebSocket sem query token | Médio no dispositivo comprometido |
| TM-08 | API aceita ID token, JWT sem assinatura ou algoritmo inesperado | Bypass de autenticação | Audience exclusiva da API; allowlist de algoritmos; assinatura/JWKS; rejeitar `none`; `typ=at+jwt` quando perfil RFC 9068 | Matriz de tokens negativos | Baixo após testes |
| TM-09 | Token de issuer/audience/ambiente diferente é aceito | Acesso cruzado entre ambientes | `iss` exato, `aud` obrigatória, clients/audiences/tenants isolados | Tokens local/homolog/prod não são intercambiáveis | Baixo |
| TM-10 | Token expirado ou ainda não válido é aceito por relógio/bypass | Sessão além da política | Validar `exp`/`nbf`; skew pequeno; sincronizar/monitorar relógio | Fixtures limítrofes e relógio divergente | Baixo |
| TM-11 | Rotação/indisponibilidade de JWKS leva a fail-open ou DoS | Bypass ou indisponibilidade | Cache por `kid`, refresh limitado, TTL; última chave válida só dentro da política; falha fechado | Rotacionar chave e indisponibilizar discovery/JWKS | Médio operacional |
| TM-12 | Access/refresh token roubado é reutilizado | Apropriação de sessão | TTL curto de access token; refresh protegido/rotacionado se suportado; revogação/logout; higiene de dispositivo | Revogar/rotacionar e testar sessão | Médio; bearer não é sender-constrained |
| TM-13 | API confia em e-mail ou claim de role mutável | Escalada de privilégio | Identidade `(iss, sub)`; membership/papel no banco; claims não concedem workspace | Alterar e-mail/role do token não muda vínculo | Baixo |
| TM-14 | Usuário troca `workspaceId`/UUID e acessa outro tenant | Vazamento/alteração horizontal | Todas as queries escopadas por membership/workspace no servidor; negar por padrão | Testes BOLA em cada endpoint e WebSocket | Alto antes dos testes; baixo depois |
| TM-15 | Identidade fixa de dev é ativada fora de local | Bypass completo | `AUTH_MODE` explícito; falhar startup fora de local; build/deploy gate | Teste de config de homolog/prod | Baixo após remoção/gate |
| TM-16 | Token aparece em query do WebSocket/proxy log | Roubo de token | Header `Authorization`, `wss`, redaction no proxy; nunca query param | Inspecionar access logs e handshake | Baixo |
| TM-17 | Evento WebSocket forjado, perdido ou fora de ordem muda estado final | Integridade incorreta da UI | Socket autenticado/autorizado; snapshot PostgreSQL como verdade; cursor/ordem/id; reconciliação após reconexão | Fixture com perda, repetição e reorder | Médio até protocolo persistente existir |
| TM-18 | Retry cria tarefas ou decisões duplicadas | Execução/approval não intencional | Idempotency key; constraint/transação; consultar resultado após timeout | Repetir request e perder resposta deliberadamente | Médio até implementação |
| TM-19 | Dois usuários decidem a mesma approval ou payload muda após consentimento | Ação não consentida/repúdio | Decisão atômica única; hash/versão de payload; estado `pending`; ator/horário auditados | Teste concorrente e mutação de payload | Alto até implementação |
| TM-20 | Cliente aprova offline e ação ocorre depois sem contexto | Ação sensível inesperada | Approvals somente online; nada de fila silenciosa; revalidar estado/risco antes do envio | Teste offline antes/durante decisão | Baixo |
| TM-21 | Device/agent declara capability privilegiada e a recebe automaticamente | Execução arbitrária | Registro/autorização administrativa; capability allowlist persistida; mínimo privilégio; approval para sensível | Capability desconhecida/negada falha | Alto até Marco 5 |
| TM-22 | Prompt/task induz agente a ler secret ou executar shell | Exfiltração/destruição | Executor sem shell arbitrário; sandbox; allowlist; secret isolation; approval não substitui policy | Casos adversariais e auditoria | Alto até device agent seguro |
| TM-23 | Artefato/log malicioso injeta UI, link ou conteúdo sensível | Phishing/exfiltração | Renderização como dados, escaping, preview limitado, sanitização e política de download | Payloads de controle/HTML/URI e secret fixtures | Médio |
| TM-24 | Logs/auditoria permitem negar ação ou são alterados | Falta de responsabilização | Auditoria no backend, IDs/correlation, ator/resultado, controle de acesso e retenção; não confiar no cliente | Reconstruir fluxo E2E e verificar acesso | Médio até hardening |
| TM-25 | Conta do IdP comprometida autoriza ação crítica | Acesso e ação indevidos | MFA/política do IdP; reautenticação/step-up futura para alto risco; alertas e revogação | Critério obrigatório na escolha do IdP prod | Médio; decisão humana pendente |
| TM-26 | Logout local é interpretado como revogação global | Token continua utilizável | TTL curto; revogação/RP logout quando suportado; mensagem/telemetria corretas | Usar token capturado após logout e observar política | Médio |

## Prioridades antes do walking skeleton

### Bloqueadores de autenticação

- TM-01 a TM-10, TM-13, TM-15 e TM-16;
- testes negativos automatizados de JWT e transação OIDC;
- nenhuma credencial fixa ativa em homologação.

### Bloqueadores de integração multi-workspace

- TM-14 em todos os endpoints, inclusive dashboard, detalhe, approvals e WebSocket;
- regra persistente para `(issuer, subject)` e membership.

### Bloqueadores de approvals/device agent

- TM-18 a TM-22;
- decisão atômica, payload vinculado, idempotência e executor allowlisted.

## Riscos aceitos temporariamente no local

- HTTP loopback e redirect por scheme privado, somente com dados/usuários descartáveis;
- Keycloak como referência sem SLA;
- simulador de eventos sem garantia operacional de produção.

Essas exceções devem ser impossíveis de ativar por acidente em homologação/produção.

## Itens que exigem decisão humana

- provedor OIDC de produção, MFA, recuperação de conta, break-glass e residência de dados;
- tolerância a risco/necessidade de step-up para aprovação de produção;
- política de retenção e acesso a auditoria/artefatos;
- bloqueio de screenshots/recents e implicações de usabilidade;
- revisão do texto que comunica risco e consequência ao aprovador.

Tokens visuais e a opção `dark-only`/`dark-first` não alteram controles deste threat model e permanecem sob revisão humana de design.

## Referências primárias

- [RFC 9700 — Best Current Practice for OAuth 2.0 Security](https://www.rfc-editor.org/rfc/rfc9700.html)
- [RFC 8252 — OAuth 2.0 for Native Apps](https://www.rfc-editor.org/rfc/rfc8252.html)
- [RFC 9068 — JWT Profile for OAuth 2.0 Access Tokens](https://www.rfc-editor.org/rfc/rfc9068.html)
- [RFC 9207 — Authorization Server Issuer Identification](https://www.rfc-editor.org/rfc/rfc9207.html)
- [OpenID Connect Core 1.0](https://openid.net/specs/openid-connect-core-1_0-18.html)
- [OpenID Connect Discovery 1.0](https://openid.net/specs/openid-connect-discovery-1_0.html)

