# Mapa de telas e navegação

**Status:** mapa funcional; não é especificação visual  
**Base observada:** rotas Compose existentes em 2026-08-24

## Grafo-alvo do fluxo P0

```text
Inicialização
  +-- sessão ausente/inválida -> Login -> navegador OIDC -> Callback
  |                                                |
  +-- sessão restaurada e validada ----------------+
                                                   v
                                            Resolver /v1/me
                                                   |
                         +-------------------------+-------------------------+
                         |                                                   |
                  sem workspace                                     com workspace
                         |                                                   |
                 Sem acesso/workspace                              Seleção de workspace
                                                                             |
                                                                             v
                            +---------------- Dashboard ----------------+
                            |                    |                       |
                            v                    v                       v
                      Lista tarefas       Lista máquinas          Aprovação pendente
                            |                    |                       |
                   +--------+--------+           v                       v
                   |                 |      Detalhe máquina       Detalhe aprovação
                   v                 v                                  |
              Nova tarefa      Detalhe tarefa <-------------------------+
                                     |
                       +-------------+-------------+
                       |             |             |
                       v             v             v
                 Detalhe agente   Artefato/diff   Resultado
```

`Sessão expirada` é uma transição global a partir de qualquer tela protegida: interrompe operações protegidas e volta ao gate de autenticação. `Offline` não é uma nova tela obrigatória; é um estado explícito da tela atual, conforme o contrato de estados.

## Inventário de destinos

| Destino funcional | Rota atual | Estado atual | Papel no MVP | Entradas principais |
|---|---|---|---|---|
| Inicialização/sessão | inexistente | pendente | P0 | lançamento do app |
| Login | inexistente | pendente | P0 | sessão ausente, logout, expiração |
| Callback OIDC | inexistente | pendente | P0 técnico, sem necessidade de tela própria | navegador externo |
| Sem acesso a workspace | inexistente | pendente | P0 alternativo | `/v1/me` sem vínculos |
| Seleção de workspace | inexistente | pendente | P0 | login, troca manual de contexto |
| Dashboard | `HomeRoute` | implementado com API remota | P0 | workspace selecionado |
| Lista de tarefas | `TaskListRoute` | implementado com API remota | P0 | dashboard, aba Tarefas |
| Nova tarefa | `NewTaskRoute` | placeholder | P0 | ação rápida/lista de tarefas |
| Detalhe da tarefa/timeline | `TaskDetailRoute(taskId)` | implementado com API remota + WebSocket | P0 | dashboard, lista, criação |
| Detalhe de aprovação | `ApprovalRoute(approvalId)` | placeholder | P0 | dashboard, detalhe da tarefa |
| Resultado | `ResultRoute(taskId)` | placeholder | P0 | detalhe de tarefa concluída |
| Lista de máquinas | `MachineListRoute` | placeholder | P0 leitura | dashboard, aba Máquinas |
| Detalhe de máquina | `MachineDetailRoute(machineId)` | placeholder | P0 leitura | dashboard, lista de máquinas |
| Detalhe de agente | `AgentDetailRoute(agentId)` | placeholder | P0 leitura | dashboard, detalhe da tarefa |
| Arquivos/artefatos | `FilesRoute(machineId)` | placeholder | P0 somente como artefato seguro | resultado/detalhe da tarefa |
| Diff | `CodeDiffRoute(filePath)` | placeholder | P0 opcional como artefato somente leitura | resultado/artefato |
| Terminal/SSH | `TerminalRoute(machineId)` | placeholder | fora do primeiro corte | não expor no fluxo P0 |

“Implementado” indica apenas existência da tela observada; não significa integração com API, autenticação ou aceite de design.

## Regras de navegação

1. Rotas protegidas só entram no grafo depois de a sessão ser validada e o usuário resolvido pela API.
2. `workspaceId`, `taskId`, `approvalId`, `agentId` e `machineId` são referências, nunca autorização. A API valida o vínculo em toda chamada.
3. Após login com um único workspace, o app pode selecioná-lo automaticamente; com vários, pede escolha; com nenhum, mostra “sem acesso”.
4. Trocar workspace limpa o back stack de detalhes do workspace anterior e reinicia seus streams/cache.
5. Deep links para conteúdo protegido passam primeiro pelo gate de sessão e, após autenticação, só retomam se o recurso pertencer a workspace autorizado.
6. Concluir a criação abre o detalhe da tarefa retornada pelo servidor. Reenvio deve usar idempotência, não navegação duplicada.
7. Aprovar/rejeitar retorna ao estado persistido da aprovação e da tarefa; não presume sucesso antes da resposta da API.
8. Resultado e artefato são alcançados a partir da tarefa. Caminho de arquivo não deve ser tratado como identificador global nem aceito sem validação no backend.
9. Logout limpa o back stack protegido.
10. A ação rápida “Perguntar à IA” e ações de upload/terminal ficam ocultas ou marcadas indisponíveis no MVP enquanto não houver contrato seguro.

## Navegação de alto nível existente

O app atual possui abas `Home`, `Tarefas`, `Máquinas` e `Arquivos`, além de uma ação central. Esse arranjo pode continuar como hipótese de implementação, mas sua composição e aparência ainda precisam de validação humana. O contrato P0 exige acesso previsível a dashboard, tarefas e intervenção; não exige essas quatro abas específicas.

## Conteúdo mínimo por tela P0

| Tela | Conteúdo/ações mínimos |
|---|---|
| Login | motivo do acesso, ação Entrar, erro recuperável e política/ajuda quando disponível |
| Seleção de workspace | nome dos vínculos autorizados, última seleção como preferência e ação de troca |
| Dashboard | workspace ativo, aprovações pendentes, tarefas recentes, agentes, máquinas e horário de atualização |
| Nova tarefa | projeto autorizado, objetivo, validação, enviar uma vez e cancelar |
| Detalhe da tarefa | status, progresso, agente, máquina, timeline, intervenção pendente e resultado |
| Aprovação | ação exata, alvo/ambiente, risco, consequência, reversão, solicitante, expiração/estado e aprovar/rejeitar |
| Resultado | desfecho, resumo, timestamps e acesso aos artefatos sanitizados |

## Revisão humana pendente

- tokens de cor, tipografia, espaçamento, shapes, ícones e animações;
- decisão `dark-only` versus `dark-first`;
- composição da navegação de alto nível e comportamento em telas pequenas;
- validação de clareza/risco nas telas Login, Dashboard, Aprovação e Resultado.
