# Artigo para LinkedIn — Work Control

## Versão pronta para publicação

### Estou transformando vibecoding em um produto real — e a primeira correção foi parar de correr

Nos últimos dias comecei a tirar do papel uma ideia chamada **Work Control**: um cockpit mobile para acompanhar e controlar trabalhos executados por agentes de IA em computadores e servidores.

A proposta não é criar mais um chatbot.

A unidade principal é o trabalho:

**Workspace → Projeto → Tarefa → Agentes → Execuções → Resultados**

Imagine iniciar uma tarefa pelo celular, acompanhar diferentes agentes trabalhando, saber em qual máquina cada execução está acontecendo, receber logs e diffs e ser chamado quando uma ação sensível exigir aprovação humana.

Essa é a visão.

Como muitos projetos pessoais, ele começou com bastante vibecoding. Em pouco tempo eu já tinha um protótipo visual, um app Android em Kotlin/Compose, uma API em Go, PostgreSQL, WebSocket e uma arquitetura planejada com um orquestrador Java.

Parecia um grande avanço — e era. Mas havia uma diferença importante entre **ter código gerado** e **ter um produto validado**.

Eu ainda não tinha executado o APK no meu próprio fluxo de validação. Parte do Android continuava usando dados fake. A API possuía uma identidade fixa de desenvolvimento. A documentação já não representava completamente o código existente. E o design tinha uma direção visual, mas ainda não uma experiência formalizada.

Foi aí que decidi mudar o método sem abandonar a velocidade.

Em vez de continuar adicionando telas e funcionalidades, defini um primeiro objetivo vertical:

**Login → workspace → dashboard real → tarefa → eventos → aprovação → resultado**

O primeiro marco agora é menos glamouroso, mas muito mais importante:

- tornar o ambiente reproduzível;
- executar o app em um aparelho ou emulador;
- validar a API e o banco localmente;
- alinhar documentação e implementação;
- registrar decisões e evidências;
- só então avançar para autenticação real.

A primeira instalação em um aparelho físico já mostrou por que essa etapa importa. O build estava verde e o app abria normalmente, mas a barra inferior estava atrás da navegação de três botões do Android. O primeiro toque destinado à aba de tarefas acionou o sistema operacional.

A correção foi pequena — respeitar os insets da barra de navegação —, mas o aprendizado é grande: **um APK gerado não significa uma experiência validada**. Há problemas que só aparecem quando o software encontra um dispositivo, uma configuração e uma pessoa reais.

Essa fundação de identidade já começou a sair do papel. Configurei um Keycloak
local reproduzível, modelei identidades por `(issuer, subject)` e implementei na
API a validação de access tokens via OpenID Connect Discovery/JWKS. O cliente
Android é público, sem segredo no APK, e o fluxo de referência usa Authorization
Code com PKCE S256.

O primeiro smoke com um token real encontrou algo que os testes sintéticos não
tinham mostrado: o token do realm não carregava o claim `sub`. A API recusou a
requisição, como deveria. Corrigi o mapper do provedor e repeti o fluxo completo
— login, PKCE, troca do code, validação do token e `/v1/me` — sem registrar o
token em logs. Esse tipo de falha é exatamente o motivo de integrar cedo.

Autenticação, porém, não é autorização. O próximo corte precisa restringir cada
recurso ao membership/workspace correto antes de qualquer exposição externa, e
o login/sessão ainda precisa chegar ao Android real.

Também estou mantendo uma regra de produto simples para cada tela de execução:

**O que está acontecendo? Quem está fazendo? Onde está rodando? Qual foi o resultado? Precisa de mim?**

Essa regra está guiando tanto o design quanto a arquitetura.

O maior aprendizado até aqui é que vibecoding não precisa significar desenvolvimento sem direção. IA acelera muito a criação, mas alguém ainda precisa definir fronteiras, critérios de aceite, riscos, sequência e o que realmente prova valor para o usuário.

Meu próximo objetivo não é ter mais código.

É levar o login ao Android e construir a menor versão do Work Control que
funcione de ponta a ponta, com isolamento por workspace, segurança suficiente
para merecer confiança e documentação suficiente para poder evoluir.

Vou compartilhar os próximos marcos, inclusive os erros e decisões que aparecerem no caminho.

Quem já está construindo produtos com agentes de IA: qual foi o momento em que o seu protótipo deixou de ser uma demonstração e começou a parecer um produto?

#InteligenciaArtificial #DesenvolvimentoDeSoftware #Android #Golang #OAuth2 #OpenIDConnect #BuildInPublic #VibeCoding #AgentesDeIA

## Nota antes de publicar

Não usar capturas do aparelho físico que incluam barra de status, notificações ou outros aplicativos. Produzir as imagens em emulador limpo ou recortar somente a área segura do Work Control. Sugestões:

1. captura do dashboard no Android;
2. diagrama simples do fluxo principal;
3. foto ou mockup do celular acompanhando uma execução.
