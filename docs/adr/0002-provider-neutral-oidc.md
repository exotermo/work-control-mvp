# ADR 0002 — Autenticação OIDC e autorização da aplicação

**Status:** proposto para implementação  
**Data:** 2026-08-24  
**Decisões humanas pendentes:** provedor de produção e requisitos comerciais/operacionais de identidade

## Contexto

A API atual injeta um usuário e workspace fixos para desenvolvimento. O Android ainda não possui sessão e começa diretamente em uma rota protegida. O MVP precisa autenticar uma pessoa em um app nativo e autorizar separadamente seu acesso aos recursos de cada workspace.

OAuth 2.0 sozinho delega acesso; OpenID Connect acrescenta a camada interoperável de identidade. O Android é um cliente nativo público: qualquer segredo distribuído no APK deve ser considerado recuperável. O sistema também deve trocar o provedor de identidade sem reescrever as regras de domínio.

## Decisão

Adotar OpenID Connect sobre OAuth 2.0 com **Authorization Code Flow + PKCE `S256`** para o Android, usando navegador externo/Custom Tab. O desenho usa apenas contratos padronizados de discovery, authorization, token, JWKS e logout/revogação quando anunciados.

Keycloak será o IdP de referência para desenvolvimento local e testes de integração. Isso não o torna o provedor escolhido para homologação ou produção. A escolha do IdP de produção requer decisão humana considerando segurança, MFA, residência de dados, SLA, custo, suporte, operação e estratégia de recuperação.

### Responsabilidades

```text
Android (cliente público)
  -> inicia autenticação e valida a transação local
  -> mantém sessão protegida
  -> trata access token como opaco
  -> envia Bearer token somente à audience da API

IdP / Authorization Server
  -> autentica a pessoa
  -> emite authorization code, ID token, access token e, se permitido, refresh token
  -> publica metadata e chaves

API Go / Resource Server
  -> valida access token
  -> resolve identidade externa por (issuer, subject)
  -> consulta vínculo, papel e políticas no banco
  -> autoriza cada recurso/ação e produz auditoria
```

Autenticação no IdP não concede automaticamente acesso a workspace. Claims de grupo/papel podem auxiliar provisionamento futuro, mas não substituem `workspace_members`, capabilities, approvals e políticas persistidas pela aplicação.

## Fluxo normativo

1. O app carrega uma configuração de ambiente com `issuer`, `client_id`, `audience`, `scopes` e `redirect_uri`. O `issuer` não vem de entrada livre do usuário.
2. O cliente obtém metadata por Discovery e exige que o `issuer` retornado corresponda exatamente ao configurado.
3. Para cada tentativa, o app gera `code_verifier`, `code_challenge` `S256`, `state` e `nonce` com aleatoriedade criptográfica e vínculo à transação.
4. O app abre o authorization endpoint em navegador externo. WebView embutida não é aceita.
5. O IdP compara o redirect URI com o valor pré-registrado exato e retorna um authorization code.
6. O app só aceita o callback esperado; valida `state`, issuer da resposta quando suportado e, no ID token, assinatura, `iss`, `aud`, `exp` e `nonce` antes de considerar a autenticação concluída.
7. O app troca o code usando o `code_verifier`. O cliente Android não envia nem armazena client secret.
8. O app usa o ID token para o evento de autenticação/sessão do cliente e o access token para chamar a API. A API nunca aceita ID token como credencial de acesso.
9. O access token segue no header `Authorization: Bearer`; não vai em query string, inclusive no WebSocket.
10. A API valida o token e resolve `(iss, sub)` para uma identidade externa. E-mail e nome são atributos mutáveis, não chaves de segurança.
11. A API resolve workspaces/papéis pelo banco e decide a ação. Token válido sem permissão produz `403`; token ausente/inválido produz `401`.
12. Renovação ocorre antes da expiração apenas se o ambiente emitir refresh token. Falha terminal de refresh expira a sessão. Logout limpa estado local e usa revogação/RP-Initiated Logout se publicados e aplicáveis.

## Contrato de validação da API

A API mantém allowlist de issuer por ambiente e, para access tokens JWT:

- fixa algoritmos de assinatura aceitos; nunca deriva a política apenas do header do token e nunca aceita `alg=none`;
- seleciona uma chave publicada pelo issuer e suporta rotação controlada de JWKS;
- exige assinatura válida, `iss` exatamente igual ao configurado, `aud` contendo a audience da API, `exp` futuro e `sub` não vazio;
- valida `nbf`, `iat`, `client_id`/`azp` e scopes quando presentes e requeridos pelo perfil selecionado;
- impede confusão entre ID token e access token pela audience distinta e, quando o IdP emite o perfil RFC 9068, exige `typ=at+jwt`;
- falha fechado se metadata/JWKS não puderem ser renovados e não houver chave válida ainda em cache;
- aceita pequena tolerância de relógio configurada e monitorada, nunca desabilita expiração;
- não registra token nem claims pessoais desnecessários.

O cliente Android trata o access token como opaco. Se um futuro provedor usar token opaco, suportá-lo exigirá uma extensão explícita deste ADR e introspecção segura; não haverá fallback automático entre JWT e token opaco.

### Estado do incremento inicial da API

O incremento atual implementa discovery/JWKS, allowlist de algoritmo, assinatura,
`iss`, `aud`, `exp`, `nbf`, `sub`, scope e o claim `typ=Bearer` emitido pelo
Keycloak local. `iat`, `client_id`/`azp`, TTL/telemetria de cache JWKS e skew
configurável permanecem gates obrigatórios antes de homologação. Provedores que
usem o header RFC 9068 `typ=at+jwt` exigem perfil explícito; não há relaxamento
automático da validação.

## Contrato de autorização

- A chave externa é única por `(issuer, subject)` e se relaciona a um `user` interno.
- O workspace ativo é uma escolha de contexto, não um claim de confiança enviado pelo cliente.
- Toda query de recurso inclui ou deriva o workspace autorizado no servidor.
- Scopes concedem acesso grosso à API; permissões de negócio continuam no banco.
- Aprovação exige membership/papel apropriado e é vinculada a payload/ação imutável.
- Dispositivos e agentes terão identidades próprias. Access token humano não autentica device agent.
- A identidade fixa atual só pode existir sob chave de desenvolvimento explícita e deve falhar ao iniciar fora de `local`.

## Android e armazenamento

Usar uma biblioteca nativa aderente ao RFC 8252; [AppAuth for Android](https://github.com/openid/AppAuth-Android) é a referência inicial porque usa navegador/Custom Tabs, suporta discovery e PKCE e não depende de Keycloak. A versão da dependência será decidida e fixada na implementação.

O estado durável de autorização deve ser cifrado com chave protegida pelo Android Keystore. Material de curta duração (`state`, `nonce`, verifier) também deve sobreviver apenas ao ciclo necessário para validar o callback. Tokens não entram em Room, backup Android, clipboard, analytics ou logs. Screenshots/recents de telas sensíveis serão avaliados na implementação.

### Redirect URI

- Local/debug pode usar scheme privado baseado no application ID, com risco de interceptação residual aceito somente no ambiente local e mitigado por PKCE.
- Homologação e produção devem usar Android App Links HTTPS verificados em domínio controlado. O domínio publica `/.well-known/assetlinks.json` para o package e certificado corretos.
- Cada ambiente usa client registration, redirect e credenciais operacionais isolados; wildcard de redirect é proibido.

Os valores e pendências estão em [oidc-environment-matrix.md](../security/oidc-environment-matrix.md).

## WebSocket

O handshake usa `Authorization: Bearer` sobre `wss`/TLS (ou `ws` somente na exceção local). Access token em query string é proibido porque URLs aparecem em logs e histórico. Ao receber `401`, o cliente tenta uma única renovação coordenada antes de reabrir o socket. Após reconectar, busca snapshot/eventos persistidos para cobrir lacunas.

## Consequências

### Positivas

- separa identidade, autorização de domínio e identidade de dispositivo;
- evita segredo estático no APK e fluxo implicit;
- permite trocar IdP por configuração e um perfil de token documentado;
- cria contratos testáveis para `401`, `403`, isolamento de workspace e expiração;
- mantém login/MFA no user-agent controlado pelo usuário e pelo IdP.

### Custos e riscos residuais

- é necessário provisionar client/audience/redirect por ambiente;
- App Links exigem domínio, certificado de assinatura e `assetlinks.json` corretos;
- tokens bearer roubados podem ser reutilizados até expiração/revogação; TTL, armazenamento e TLS limitam, mas não eliminam o risco;
- logout local não garante revogação instantânea de todo access token já emitido;
- disponibilidade do IdP/JWKS afeta novos logins e rotação de chaves;
- o perfil JWT exato precisa ser validado em testes de conformidade de cada IdP candidato.

## Alternativas rejeitadas

- **Identidade fixa:** útil somente como bootstrap local explícito; não autentica pessoas.
- **Usuário/senha coletados pelo app:** amplia exposição de credenciais e acopla autenticação ao produto.
- **WebView embutida:** reduz isolamento e facilita captura/phishing de credenciais.
- **Implicit Flow:** expõe tokens na resposta de autorização e foi substituído pelo code flow com PKCE.
- **Client secret no APK:** cliente nativo não consegue manter esse segredo confidencial.
- **E-mail como identificador:** é mutável e pode ser reciclado; usar `(iss, sub)`.
- **Roles do IdP como autorização completa:** acopla domínio ao provedor e enfraquece isolamento de workspace.
- **Token na URL do WebSocket:** aumenta vazamento em logs, proxies e histórico.

## Critérios para aceitar este ADR na implementação

- threat model revisado antes de remover a identidade fixa;
- matriz com valores concretos do ambiente alvo, sem placeholder em release;
- discovery e rotação de JWKS testados;
- testes negativos para assinatura, algoritmo, `iss`, `aud`, `exp`, `nonce/state` e ID-token confusion;
- testes de acesso horizontal entre workspaces;
- inspeção automatizada/manual de logs para ausência de tokens;
- escolha humana e due diligence do provedor antes da produção.

## Referências normativas e primárias

- [OpenID Connect Core 1.0](https://openid.net/specs/openid-connect-core-1_0-18.html)
- [OpenID Connect Discovery 1.0](https://openid.net/specs/openid-connect-discovery-1_0.html)
- [RFC 8252 — OAuth 2.0 for Native Apps](https://www.rfc-editor.org/rfc/rfc8252.html)
- [RFC 7636 — Proof Key for Code Exchange](https://www.rfc-editor.org/rfc/rfc7636.html)
- [RFC 9700 — Best Current Practice for OAuth 2.0 Security](https://www.rfc-editor.org/rfc/rfc9700.html)
- [RFC 9068 — JWT Profile for OAuth 2.0 Access Tokens](https://www.rfc-editor.org/rfc/rfc9068.html)
- [RFC 9207 — Authorization Server Issuer Identification](https://www.rfc-editor.org/rfc/rfc9207.html)
- [Keycloak — endpoints OIDC](https://www.keycloak.org/securing-apps/oidc-layers)
- [Android Developers — Verify App Links](https://developer.android.com/training/app-links/verify-applinks)
