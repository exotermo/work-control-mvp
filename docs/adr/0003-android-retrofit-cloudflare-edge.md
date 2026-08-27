# ADR 0003 — Retrofit no Android e Cloudflare na borda

**Status:** aceito para o MVP local  
**Data:** 2026-08-24

## Contexto

O Android possuía interfaces de repositório, mas o runtime usava apenas dados
in-memory. A API Go já expunha REST e WebSocket. A zona de DNS disponível é
`xandehome.api.br` e a borda pretendida usa Cloudflare Tunnel/Zero Trust.

O projeto permanece em Kotlin `2.0.21`. Retrofit `3.0.0` foi publicado em Kotlin
`2.1.21`; atualizar todo o toolchain não pertence a este marco.

## Decisão

- Retrofit `2.12.0`, última linha 2.x, para REST com funções `suspend`.
- Converter Gson `2.12.0`, alinhado ao contrato JSON atual da API e à arquitetura
  de referência informada pelo mantenedor.
- OkHttp `4.12.0` compartilhado por Retrofit e WebSocket.
- Clean Architecture pragmática: DTO → mapper → modelo de domínio; ViewModels não
  conhecem Retrofit, Gson, status HTTP ou corpo bruto de erro.
- `Authorization: Bearer` é adicionado por interceptor a partir de um provider de
  sessão. O store atual é apenas em memória; login e persistência protegida seguem
  no Marco 2.
- Erros `401`, `403`, `404`, `409`, `429`, rede, `5xx` e protocolo são traduzidos
  para falhas de domínio com mensagens sanitizadas.
- WebSocket usa o mesmo OkHttp/interceptor, reconecta com backoff limitado e trata
  evento somente como invalidação; o snapshot REST/PostgreSQL continua verdadeiro.
- Debug usa `http://localhost:8080/` e cleartext somente no manifest debug. Release
  usa HTTPS e default `https://work-control.xandehome.api.br/`.
- Room não será introduzido sem requisito offline mensurável.

## Cloudflare

O Tunnel oculta a origem e encaminha o hostname para a API em loopback. Cloudflare
Access pode proteger superfícies administrativas ou uma beta com WARP, mas service
tokens não são distribuídos no app. O endpoint mobile continua exigindo OIDC na API.

Publicar DNS/Tunnel fica bloqueado até os testes de autorização por
membership/workspace/BOLA cobrirem REST e WebSocket.

## Consequências

- Runtime deixa de usar fakes; fakes ficam em `src/test`.
- O app debug exige a stack local e `adb reverse tcp:8080 tcp:8080` no aparelho.
- A sessão OIDC Android ainda precisa alimentar o provider de token antes do uso
  de `AUTH_MODE=oidc` no app.
- Retrofit 3 poderá ser avaliado junto a uma atualização deliberada do toolchain.

## Evidência

- `testDebugUnitTest assembleDebug assembleRelease`: sucesso, 13 testes e R8.
- MockWebServer valida Bearer sem query, contratos DTO/repositórios, erros e
  reconexão WebSocket.
- Smoke real retornou JSON válido em `/v1/me`, `/v1/tasks`, `/v1/agents`,
  `/v1/devices` e `/v1/approvals` na API local.

Referências: [Retrofit releases](https://github.com/square/retrofit/releases),
[OkHttp](https://square.github.io/okhttp/),
[Cloudflare Tunnel](https://developers.cloudflare.com/tunnel/setup/).
