# Mapa Android → Prelo Control

O Android lê e escreve diretamente no Prelo. O servidor é a autoridade de identidade, projetos, tasks, execuções, aprovações e deploys. `X-Project-Id` representa apenas a seleção de interface; a autorização é validada a cada chamada pelo Prelo.

| Tela | Recurso Prelo | Estado no app |
| --- | --- | --- |
| Login e sessões | `dashboard-auth/login`, `mobile/verify|refresh|logout`, `/me`, `/me/sessions` | Ligada. Refresh local cifrado com Android Keystore. |
| Home | `GET /api/v1/home` | Ligada; SSE e polling. |
| Tarefas | `/tasks`, detalhe, árvore, execução e turnos | Ligada; criação e execução assíncrona. |
| Aprovações | `/approvals`, decisão | Ligada; `step_up_required` pede TOTP. |
| Máquinas | `/servers`, detalhe, health-check | Ligada; sem CPU/RAM inventados. |
| Pipeline | `/pipeline` | Ligada. |
| Deploys | `/projects/{id}/actions?kind=deploy` | Leitura ligada; sem execução no app. |
| Arquivos | `/projects/{id}/files`, `/files/{fileId}/content` | Listagem e download ligados. |
| Terminal e diff | Nenhum endpoint contratado | Removidos da navegação. |

SSE: `GET /api/v1/events/stream` apenas em primeiro plano, com reconexão e polling reserva. Push: `PUT|DELETE /api/v1/me/push-token`, com FCM opcional. O servidor está inicialmente com `pushEnabled=false`. A tela sempre consulta o estado atual via API após aviso SSE ou push.
