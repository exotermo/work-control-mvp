# Mapa de migração Android → Prelo Control

Levantamento do código Android e dos contratos em `~/projects/hermes/docs/` em 2026-10-04. O Prelo já implementa sessão mobile (PR-2) e `action-requests` (PR-3, resposta v1.1); as telas Android ainda precisam ser conectadas e testadas contra o servidor ativo.

| Tela | Recurso atual | Equivalente Prelo | Estado |
| --- | --- | --- | --- |
| Home | `GET /v1/me`, `/v1/tasks`, `/v1/agents`, `/v1/devices`, `/v1/approvals` da API Go | `GET /api/v1/me`, tasks, agents, servers, approvals; `X-Project-Id` | Migrável agora para leitura, com sessão pessoal válida; polling para atualização. |
| Lista de tarefas | `GET /v1/tasks` | `GET /api/v1/tasks` + `X-Project-Id` | Migrável agora, com adaptação dos DTOs. |
| Detalhe da tarefa | `GET /v1/tasks/{id}`, `WS /v1/tasks/{id}/events/ws` | `GET /api/v1/tasks/{id}`, árvore, execução e turnos | Migrável com polling; WebSocket não existe no Prelo. |
| Agente | Tela placeholder; `/v1/agents` só na Home | `GET /api/v1/agents`; execução/turnos para atividade | Migrável para catálogo; atividade por agente sem equivalente direto de tela. |
| Aprovação | Tela placeholder e ID fixo no botão da Home; `/v1/approvals` no repositório | `GET /api/v1/approvals`, `GET /{id}`, `POST /{id}/approve|deny` | Migrável; aprovação HIGH mobile exige TOTP recente conforme PR-2. |
| Máquinas | Tela placeholder; `/v1/devices` no repositório | `GET /api/v1/servers`, `GET /{id}`, health-check | Leitura migrável; CPU/RAM/dispositivo não têm equivalente no contrato atual. |
| Nova tarefa | Tela placeholder; `POST /v1/tasks` no repositório | `POST /api/v1/tasks`, `POST /{id}/execute` | Migrável agora com sessão pessoal válida. |
| Resultado | Tela placeholder | `GET /api/v1/tasks/{id}/executions/{executionId}` | Migrável com polling após criação/execução. |
| Arquivos | Tela placeholder | Rotas de arquivos do projeto | Migrável para arquivos do projeto; arquivos arbitrários de dispositivo sem equivalente conhecido. |
| Diff de código | Tela placeholder | Sem diff de código no contrato atual | Sem equivalente conhecido. |
| Terminal | Tela placeholder e ID de máquina fixo | Nenhum endpoint de shell/SSH do Prelo | Bloqueada por contrato e política de risco; não simular acesso. |
| Pipeline visual | Nenhuma tela dedicada | `GET /api/v1/pipeline`, árvore por tarefa | Migrável com polling e seleção de projeto. |
| Deploys | Nenhuma tela | `GET /api/v1/projects/{projectId}/actions?kind=deploy&limit=50` | Migrável com polling; modelo de resposta v1.1 disponível, UI pendente. |
| Login/sessões | Nenhuma tela funcional; bearer em memória e Keycloak só no backend Go | `dashboard-auth/login`, `mobile/verify|refresh|logout`, `/me/sessions` | Contrato e servidor implementados; UI e armazenamento cifrado no Keystore pendentes. |

**Direção de migração:** o Android fala diretamente com o Prelo. A API Go, Keycloak, orquestrador Java e device agent atuais não são introduzidos no fluxo novo. Nenhuma tabela deles é apagada neste incremento. `403` do Prelo é decisão final do servidor, sem recalcular autorização no app.
