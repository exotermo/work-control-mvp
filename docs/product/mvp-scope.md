# Contrato de escopo do MVP

**Status:** proposto para execução  
**Última revisão documental:** 2026-08-24

## Objetivo de validação

O MVP deve responder a uma pergunta de produto: uma pessoa desenvolvedora consegue usar o celular como cockpit para iniciar e supervisionar um trabalho executado por um agente, intervir em uma operação sensível e verificar o resultado sem permanecer diante da máquina?

O fluxo que materializa essa hipótese é:

```text
autenticar
  -> selecionar workspace autorizado
  -> identificar uma máquina disponível
  -> criar uma tarefa
  -> acompanhar execução e eventos
  -> decidir uma aprovação bloqueante
  -> consultar resultado/artefato e trilha de auditoria
```

O produto não é um chatbot. Conversa pode existir como mecanismo auxiliar no futuro, mas a unidade de trabalho do MVP é `Workspace -> Projeto -> Tarefa -> Execução -> Resultado`.

## Dentro do MVP

| Capacidade | Corte mínimo verificável |
|---|---|
| Autenticação | Login OIDC pelo navegador externo, renovação/expiração de sessão e logout |
| Autorização | A API resolve usuário por `(issuer, subject)` e permite somente workspaces dos quais ele é membro |
| Workspace | Listar e selecionar manualmente um workspace autorizado; localização pode apenas sugerir contexto |
| Dashboard | Exibir tarefas recentes/em andamento, agentes ativos, máquinas e aprovações pendentes com dados reais |
| Dispositivo | Registrar uma máquina, anunciar capabilities e manter heartbeat/presença |
| Tarefa | Criar uma tarefa com objetivo textual e consultar lista/detalhe |
| Execução | Executar uma única ação previamente permitida, associada a agente e máquina identificáveis |
| Tempo real | Receber eventos ordenados da tarefa e recuperar o estado após reconexão |
| Aprovação | Bloquear uma operação sensível, explicar risco/consequência, aceitar uma decisão humana única e auditá-la |
| Resultado | Exibir status final e ao menos um artefato seguro, como resumo, log sanitizado, relatório de testes ou diff |
| Auditoria | Registrar ator, workspace, ação abstrata, alvo, horário, resultado e aprovação relacionada |
| Operação | Build/instalação reproduzível, ambiente de homologação isolado, smoke test e logs sem secrets/tokens |

## Não escopo do primeiro corte

- marketplace ou instalação dinâmica de agentes;
- múltiplos provedores de IA ativos simultaneamente;
- editor de código completo no celular;
- desktop remoto ou streaming de tela;
- terminal/SSH arbitrário e execução de comandos enviados livremente pelo cliente;
- deploy irrestrito em produção;
- sincronização bidirecional ou upload complexo de arquivos;
- automações/workflows configuráveis pelo usuário;
- colaboração multiusuário em tempo real além do modelo mínimo de membros;
- seleção automática de workspace que produza autorização;
- funcionamento offline para criar, aprovar ou executar ações;
- cache persistente com Room sem requisito mensurável;
- paridade funcional entre Android, web e desktop;
- escolha definitiva do provedor OIDC de produção.

Telas existentes para terminal, arquivos, diff, agente e máquina podem permanecer como placeholders enquanto não fizerem parte do fluxo mínimo. Uma visualização somente leitura de diff ou log pode ser usada como artefato; isso não autoriza shell ou edição remota.

## Invariantes de produto e segurança

1. Localização, rede, IP, SSID e presença física são sinais de contexto, nunca prova de identidade ou permissão.
2. Toda consulta e mutação de domínio é limitada a um workspace autorizado no backend.
3. Capabilities de agente/dispositivo são allowlists; ausência de capability significa negar.
4. Operação sensível pausa antes do efeito e exige aprovação humana válida.
5. Aprovação é vinculada à ação concreta e não pode ser reaproveitada para payload diferente.
6. O cliente nunca envia comando arbitrário diretamente a uma máquina.
7. Tokens e secrets não entram em URL, log, evento de analytics, banco em texto puro ou artefato.
8. PostgreSQL é a fonte persistente de verdade. Cache, presença e stream transitório não podem alterar uma decisão persistida.
9. Em qualquer detalhe de execução deve ser possível responder: o que ocorre, quem executa, onde executa, qual é/foi o resultado e se há intervenção pendente.

## Métricas de validação inicial

O MVP está pronto para uma sessão de validação quando, em ambiente de homologação:

- uma pessoa autorizada completa o fluxo principal sem identidade fixa de desenvolvimento;
- um usuário sem vínculo recebe `403` ao tentar acessar outro workspace;
- um token inválido ou expirado recebe `401`;
- a criação da tarefa gera um identificador persistente e um primeiro evento;
- a perda e retomada de conexão não cria tarefa ou decisão duplicada;
- uma ação sensível não começa antes de uma decisão válida;
- o resultado pode ser relacionado à tarefa, execução, agente e dispositivo;
- a trilha de auditoria permite reconstruir o fluxo demonstrado;
- logs verificados não contêm access token, refresh token, authorization code ou secret.

## Decisões que exigem revisão humana

Os itens abaixo não estão aprovados por este contrato e não bloqueiam a implementação dos estados e fluxos funcionais:

- **tokens visuais:** cor, tipografia, espaçamento, shapes, iconografia e motion;
- **tema:** escolha entre `dark-only`, `dark-first` com modo claro posterior, ou suporte simultâneo;
- **composição em telas pequenas:** densidade, hierarquia e alvos de toque;
- **linguagem de risco:** texto final usado em aprovações e alertas;
- **provedor OIDC de produção:** contratação, residência de dados, SLA, MFA, custo e operação.

Até essa revisão, documentos de produto definem somente conteúdo, comportamento, prioridade e semântica dos estados; não prescrevem aparência.

