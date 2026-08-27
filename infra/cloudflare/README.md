# Cloudflare Tunnel / Zero Trust

O hostname adotado para o Work Control é `work-control.xandehome.api.br`. Ele é
uma convenção sobre a zona informada (`xandehome.api.br`) e pode ser trocado via
`API_BASE_URL` no build release sem alterar código.

## Modelo escolhido

```text
Android -- HTTPS/WSS --> Cloudflare edge -- Tunnel --> 127.0.0.1:8080 (api-go)
   |
   +-- Authorization: Bearer <token OIDC do usuário>
```

- `cloudflared` inicia conexões de saída; a origem continua sem porta pública.
- A API valida o access token OIDC e autoriza membership/workspace no servidor.
- Cloudflare Access protege hosts/rotas administrativos e aplicações internas.
- Para beta estritamente interna, a alternativa preferida é rota privada pelo
  Tunnel + dispositivos inscritos no WARP, somada ao OIDC da API.
- Um service token do Cloudflare é credencial máquina-a-máquina. O par
  `CF-Access-Client-Id`/`CF-Access-Client-Secret` **não entra no APK**.

Uma aplicação HTTP protegida por Access normalmente exige a sessão/cookie
`CF_Authorization`. O app Android ainda não implementa essa segunda sessão.
Portanto, não habilitar uma policy Access que exija service token no hostname
mobile: isso incentivaria embutir um segredo extraível. Usar WARP para a beta
interna ou manter o endpoint mobile no Tunnel/WAF com OIDC obrigatório na API.

## Preparação local, sem credenciais

[`tunnel-config.example.yml`](tunnel-config.example.yml) é somente um modelo.
Copie para um caminho operacional fora do repositório e substitua os placeholders.
O JSON de credenciais deve ficar fora do Git, com permissão mínima para o usuário
do serviço `cloudflared`.

Validar regras antes de iniciar:

```bash
cloudflared tunnel --config /caminho/seguro/config.yml ingress validate
cloudflared tunnel --config /caminho/seguro/config.yml ingress rule \
  https://work-control.xandehome.api.br/v1/me
```

Depois de criar o tunnel pelo painel/CLI, a rota DNS é semelhante a:

```bash
cloudflared tunnel route dns <NOME_OU_UUID_DO_TUNNEL> \
  work-control.xandehome.api.br
```

Não executar a publicação ainda. O gate atual é corrigir e testar isolamento de
workspace/BOLA em todos os endpoints e no WebSocket. O tunnel também deve rodar
no mesmo host da API para alcançar `127.0.0.1:8080`; dentro de outro container,
esse endereço apontaria para o próprio container.

Referências oficiais:

- [Cloudflare Tunnel: setup](https://developers.cloudflare.com/tunnel/setup/)
- [Arquivo e validação de ingress](https://developers.cloudflare.com/tunnel/advanced/local-management/configuration-file/)
- [Service tokens do Access](https://developers.cloudflare.com/cloudflare-one/access-controls/service-credentials/service-tokens/)
- [Cookie/JWT de autorização do Access](https://developers.cloudflare.com/cloudflare-one/access-controls/applications/http-apps/authorization-cookie/)
