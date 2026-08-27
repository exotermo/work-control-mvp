# Jornada prioritária do usuário

**Status:** contrato funcional proposto  
**Persona primária:** pessoa desenvolvedora que precisa supervisionar um trabalho fora da estação principal

## Resultado esperado

Ao final da jornada, o usuário sabe que uma tarefa foi executada em uma máquina identificada, decidiu conscientemente qualquer ação sensível e consegue conferir resultado e auditoria.

## P0 — walking skeleton autenticado

| Etapa | Intenção do usuário | Resposta obrigatória do sistema | Evidência de aceite |
|---|---|---|---|
| 1. Abrir | Entrar no cockpit | Detectar ausência/presença de sessão sem mostrar dado protegido antes da validação | Inicialização não expõe conteúdo de workspace durante restauração de sessão |
| 2. Autenticar | Confirmar identidade | Abrir o provedor OIDC no navegador externo e retornar ao app por redirect registrado | Authorization Code + PKCE `S256`; nenhum client secret no APK |
| 3. Resolver identidade | Acessar sua conta | Validar access token na API, mapear `(issuer, subject)` e carregar o perfil da aplicação | `GET /v1/me` retorna usuário e vínculos autorizados |
| 4. Selecionar workspace | Escolher o contexto de trabalho | Listar apenas vínculos autorizados e persistir a seleção como preferência, não como permissão | Workspace fora dos vínculos não se torna acessível por trocar o ID no cliente |
| 5. Consultar dashboard | Entender o que precisa de atenção | Mostrar tarefas, agentes, máquinas e aprovações; cada item informa estado e última atualização | Dados vêm da API, com estados de vazio/erro/offline explícitos |
| 6. Criar tarefa | Delegar um objetivo | Validar entrada, criar uma única tarefa e abrir seu detalhe | Retry/reenvio não cria duplicata; tarefa tem criador, projeto e horário |
| 7. Acompanhar | Saber o que ocorre | Exibir snapshot persistido e incorporar eventos em tempo real, indicando agente e máquina | Reconexão recupera lacunas pela API antes de continuar o stream |
| 8. Decidir aprovação | Autorizar ou rejeitar uma ação sensível | Pausar a ação, exibir alvo, risco, consequência e política de reversão; persistir uma decisão única | Ação não executa antes da decisão; decisão concorrente não sobrescreve a primeira |
| 9. Ver resultado | Confirmar desfecho | Mostrar status final e artefato/log sanitizado associado à execução | Resultado é rastreável até tarefa, execução, agente e dispositivo |
| 10. Sair | Encerrar o uso | Limpar sessão local e executar logout/revogação suportada pelo provedor | Reabrir área protegida exige nova sessão válida |

## Caminhos alternativos obrigatórios

### Autenticação cancelada ou falha

O app retorna a uma tela não autenticada, explica que o login não terminou e permite tentar novamente. Erros de protocolo não exibem authorization code, token ou resposta bruta do provedor.

### Nenhum workspace autorizado

O usuário autenticado vê um estado vazio de acesso, sem dashboard fake. O sistema orienta a solicitar convite ou contatar um administrador. Criar workspace está fora do fluxo até existir regra explícita.

### Sessão expirada

O app interrompe chamadas protegidas, preserva apenas rascunho local não sensível quando aplicável e solicita reautenticação. Uma aprovação ou criação de tarefa nunca é reenviada automaticamente depois do novo login sem confirmação/idempotência.

### Sem conectividade

Dados previamente carregados podem ser mostrados como desatualizados, com horário da última sincronização. Criar tarefa, decidir aprovação e executar ação ficam indisponíveis até confirmação do servidor.

### WebSocket interrompido

O app marca o tempo real como desconectado, tenta reconectar com backoff e, ao voltar, consulta o snapshot/eventos persistidos antes de aceitar o stream atual. A UI não conclui uma tarefa apenas por evento transitório.

### Máquina indisponível

A tarefa pode permanecer em fila, mas o sistema não promete execução. Deve mostrar qual capability ou dispositivo falta e permitir cancelar/reprogramar quando esses comandos existirem.

### Aprovação já decidida

O segundo decisor recebe o estado persistido atual. O sistema não transforma retry em uma nova decisão e não reutiliza a aprovação em outra ação.

### Execução falhou

O detalhe informa etapa, agente, máquina, último evento confiável e resultado da tentativa. Retry automático, se existir, aparece como nova tentativa auditável.

## Prioridades posteriores

### P1 — melhorar supervisão

- filtros e busca de tarefas;
- notificações de aprovação e conclusão;
- comparação de artefatos/diffs;
- cancelamento controlado de tarefa;
- histórico completo de auditoria acessível pelo app.

### P2 — ampliar operação

- vários providers de IA;
- workflows configuráveis;
- terminal estritamente limitado por capabilities e políticas;
- colaboração avançada;
- experiência offline além de leitura de cache.

P1 e P2 não devem atrasar a validação P0.

## Pontos de revisão humana

- O conteúdo funcional desta jornada não aprova um design visual.
- Tokens visuais e a decisão `dark-only` versus `dark-first` permanecem sob revisão humana.
- O texto final de consentimento, risco e consequência nas aprovações deve ser testado com usuários.
- A escolha do IdP de produção permanece pendente; a jornada exige apenas interoperabilidade OIDC.

