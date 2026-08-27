# Matriz OIDC por ambiente

**Status:** contrato de configuração; valores de domínio do IdP ainda precisam de provisionamento  
**Atualizado em:** 2026-08-24

## Regras comuns

- Um client OIDC público distinto por ambiente; nenhum client secret no Android.
- Authorization Code Flow habilitado, PKCE obrigatório com método `S256`; implicit flow e password/direct grant desabilitados.
- Redirect URI comparado por igualdade exata, sem wildcard.
- `issuer`, `client_id`, `audience` e redirect são isolados; token de um ambiente falha nos outros.
- `openid` é obrigatório. `profile` e `email` fornecem apresentação/contato, nunca autorização.
- `work-control.api` é o scope grosso da API. Membership, papel, capabilities e approvals vêm do banco.
- `offline_access` só é pedido se refresh token estiver habilitado e protegido; não é requisito para o primeiro login integrado.
- A API recebe access token. ID token nunca é aceito como credencial da API.

## Matriz

| Campo | Local | Homologação | Produção |
|---|---|---|---|
| IdP | Keycloak de referência | OIDC compatível, candidato ainda não aprovado | **Escolha humana obrigatória; não definida** |
| `issuer` | `http://localhost:8180/realms/work-control-local`¹ | `${OIDC_ISSUER_HOMOLOG}` — URL HTTPS exata do tenant isolado | `${OIDC_ISSUER_PROD}` — URL HTTPS exata do tenant de produção |
| `client_id` | `work-control-android-local` | `work-control-android-homolog` | `work-control-android-prod` |
| API `audience` | `urn:work-control:api:local` | `urn:work-control:api:homolog` | `urn:work-control:api:prod` |
| Scopes obrigatórios | `openid profile email work-control.api` | `openid profile email work-control.api` | `openid profile email work-control.api` |
| Scope condicional | `offline_access` | `offline_access` após teste de rotação/revogação | `offline_access` após decisão de TTL, revogação e risco |
| Redirect Android | `com.workcontrol.app.debug:/oauth2redirect` | `https://${APP_LINK_HOST_HOMOLOG}/oauth2redirect` | `https://${APP_LINK_HOST_PROD}/oauth2redirect` |
| Tipo de redirect | scheme privado baseado no package debug | Android App Link verificado | Android App Link verificado |
| API | `http://localhost:8080`² | `${API_BASE_URL_HOMOLOG}` com HTTPS | `https://work-control.xandehome.api.br/`³ |
| WebSocket | `ws://localhost:8080/...`² | `wss://${API_HOST_HOMOLOG}/...` | `wss://work-control.xandehome.api.br/...`³ |
| Cadastro/tenant | realm `work-control-local`, descartável e sem usuário real | tenant/realm e dados não produtivos isolados | tenant/realm, chaves, usuários e políticas isolados |

¹ HTTP é uma exceção exclusiva de desenvolvimento no loopback. O issuer precisa ser o mesmo texto na metadata, no token, no Android e na configuração da API. Em aparelho/emulador, a topologia deve tornar esse endereço acessível sem trocar o issuer no meio do fluxo (por exemplo, com forwarding controlado). Se isso não for possível, provisionar um hostname HTTPS local estável antes da integração; não adicionar bypass de validação.

² Os endereços `localhost` pressupõem forwarding do dispositivo/emulador para o host. O runbook de implementação deverá registrar o comando/topologia usado. Essa exceção não pode chegar a homologação ou produção.

³ Hostname preparado no build/configuração de Tunnel, ainda sem publicação. BOLA
em REST/WebSocket e os demais gates de produção precisam passar antes do DNS.

## Variáveis do contrato-alvo e estado atual

| Componente | Chave | Regra | Estado no MVP |
|---|---|---|---|
| Android | `OIDC_ISSUER` | Valor fechado por variante de build; não editável pelo usuário | Pendente |
| Android | `OIDC_CLIENT_ID` | Um client público por ambiente | Pendente |
| Android | `OIDC_AUDIENCE` | Deve produzir access token destinado à API do mesmo ambiente | Pendente |
| Android | `OIDC_SCOPES` | Lista mínima da matriz; sem scopes administrativos | Pendente |
| Android | `OIDC_REDIRECT_URI` | Deve coincidir exatamente com manifest/App Link e cadastro no IdP | Pendente |
| Android | `API_BASE_URL` | HTTPS fora de local; nunca derivada de claim/token | Implementado; hostname externo ainda não publicado |
| API | `OIDC_ALLOWED_ISSUER` | Um issuer exato no MVP; allowlist explícita se houver migração futura | Implementado |
| API | `OIDC_AUDIENCE` | Audience exclusiva da API do ambiente | Implementado |
| API | `OIDC_REQUIRED_SCOPE` | `work-control.api` | Implementado |
| API | `OIDC_EXPECTED_TOKEN_TYPE` | `Bearer` no perfil Keycloak local | Implementado |
| API | `OIDC_SIGNING_ALGORITHMS` | Allowlist explícita; `RS256` local | Implementado |
| API | `OIDC_JWKS_CACHE_TTL` | Menor que política de rotação; falha fechado sem chave válida | Pendente antes de homologação |
| API | `OIDC_CLOCK_SKEW` | Pequena tolerância documentada/monitorada; não desabilita `exp` | Pendente antes de homologação |
| API | `AUTH_MODE` | `oidc`; modo fixo permitido somente em local explícito | Implementado |

Configuração não contém private key nem client secret do Android. Se um serviço confidencial for introduzido, suas credenciais terão ciclo e armazenamento próprios e não reutilizarão o client nativo.

## Perfil do Keycloak local

Criar realm `work-control-local` e client `work-control-android-local` com:

- Client authentication: `Off` (public client);
- Standard Flow: `On`;
- PKCE method: `S256`;
- Implicit Flow, Direct Access Grants e Service Accounts: `Off`;
- Valid Redirect URIs: somente `com.workcontrol.app.debug:/oauth2redirect`;
- audience mapper para `urn:work-control:api:local` no access token;
- subject mapper no access token para resolver `(issuer, subject)`;
- scope `work-control.api` concedido ao client;
- usuários exclusivamente de desenvolvimento, sem credenciais reutilizadas.

O discovery document de referência é `${issuer}/.well-known/openid-configuration`. A configuração deve ser exportável/recriável, mas não deve conter senha real no repositório.

## Gates antes de cada ambiente

### Local

- confirmar que o discovery anuncia code flow e `S256`;
- confirmar audience e separação entre ID/access token;
- validar callback em aparelho ou emulador;
- validar que identidade fixa não ativa sem modo local explícito.
- executar `automation/scripts/oidc-local-smoke.py` sem registrar o token.

### Homologação

- substituir todos os placeholders da coluna por URLs reais;
- publicar/verificar `assetlinks.json` para a assinatura de homologação;
- testar rotação de JWKS, sessão expirada, refresh/logout e indisponibilidade do IdP;
- executar testes de isolamento de workspace e varredura de logs.

### Produção

- decisão humana documentada do provedor e do modelo de suporte/recuperação;
- substituir todos os placeholders; impedir inicialização se algum persistir;
- MFA e política de autenticação aprovadas;
- TTL/refresh/revogação, acesso administrativo e break-glass aprovados;
- `assetlinks.json` com fingerprint da assinatura distribuída (inclusive Play App Signing, se usado);
- plano de rotação, indisponibilidade e migração de `(issuer, subject)` ensaiado;
- homologação e produção sem client, tenant, chaves ou usuários compartilhados.

## Decisões humanas pendentes

1. **IdP de produção:** fornecedor, tenant, SLA, MFA, suporte, custos, residência e portabilidade.
2. Domínios finais para issuer e Android App Links.
3. Necessidade de `offline_access`, duração de access/refresh token e política de revogação.
4. Política de vinculação/migração de conta ao trocar issuer.

Nenhuma dessas decisões deve ser preenchida por conveniência com credenciais ou domínio corporativo sem autorização do mantenedor.

## Referências primárias

- [RFC 8252 — OAuth 2.0 for Native Apps](https://www.rfc-editor.org/rfc/rfc8252.html)
- [RFC 9700 — OAuth 2.0 Security Best Current Practice](https://www.rfc-editor.org/rfc/rfc9700.html)
- [OpenID Connect Discovery 1.0](https://openid.net/specs/openid-connect-discovery-1_0.html)
- [Keycloak — OIDC endpoints](https://www.keycloak.org/securing-apps/oidc-layers)
- [Keycloak Server Administration — clients, redirect e PKCE](https://www.keycloak.org/docs/latest/server_admin/)
- [Android Developers — About App Links](https://developer.android.com/training/app-links/about)
