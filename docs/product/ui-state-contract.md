# Contrato de estados de interface

**Status:** contrato comportamental proposto  
**Abrangência:** telas Android protegidas e não protegidas  
**Fora deste documento:** estética, tokens visuais e layout final

## Modelo comum

Cada tela que carrega dados remotos deve representar explicitamente:

```text
content = Uninitialized | Loading | Empty | Data | Failure
connectivity = Online | Reconnecting | Offline
session = Unknown | Authenticated | Expired
mutation = Idle | Submitting | Succeeded | Failed
freshness = Fresh | Stale(lastSuccessfulSyncAt)
```

Esses eixos podem coexistir. Por exemplo, uma lista com `Data + Offline + Stale` continua visível, enquanto uma aprovação com `Data + Offline` fica somente leitura.

## Precedência

1. `session = Expired` bloqueia conteúdo protegido e mutações.
2. Uma mutação sensível já enviada deve consultar seu resultado/idempotency key antes de oferecer retry.
3. `Data + Offline/Stale` preserva contexto de leitura, mas não permite afirmar que o estado é atual.
4. `Failure` de atualização não apaga dados válidos já carregados; converte-os em `Stale` e mostra a falha de atualização.
5. `Empty` só existe após resposta bem-sucedida e autorizada. Ausência de resposta, `403` e falha de parsing não são vazio.

## Contratos por estado

### Loading

Usar quando ainda não há resposta suficiente para renderizar o conteúdo solicitado.

- informar qual conteúdo está sendo buscado sem prometer duração;
- manter navegação segura e permitir cancelamento quando aplicável;
- não mostrar dados fake como se fossem reais;
- evitar mais de um request equivalente por recomposição/rotação;
- se já há dados, mantê-los e representar atualização em segundo plano em vez de substituir tudo por loading;
- expor descrição semântica para tecnologias assistivas.

### Empty

Usar somente após sucesso confirmado com coleção/resultado vazio.

- explicar o que não existe no contexto atual;
- oferecer uma próxima ação somente se autorizada;
- distinguir “nenhum workspace permitido”, “nenhuma tarefa” e “nenhuma aprovação pendente”;
- não sugerir criar recurso se o usuário não tiver permissão;
- não tratar filtro sem resultados como ausência global de dados.

### Error

Classificar a falha antes de apresentar recuperação:

| Categoria | Comportamento |
|---|---|
| Entrada inválida | Associar mensagem ao campo; preservar valores seguros |
| Autenticação (`401`) | Transicionar para sessão expirada |
| Autorização (`403`) | Informar falta de acesso; não oferecer retry infinito |
| Não encontrado (`404`) | Informar remoção/inexistência e oferecer retorno seguro |
| Conflito (`409`) | Recarregar estado persistido; relevante para aprovação já decidida |
| Rate limit (`429`) | Respeitar `Retry-After` quando presente e limitar tentativas |
| Servidor/rede | Preservar dados anteriores como stale; permitir retry controlado |
| Protocolo/parsing | Mensagem genérica ao usuário e diagnóstico sanitizado para operação |

Mensagens ao usuário não exibem stack trace, corpo bruto, token, URL com credencial ou detalhe interno. Logs usam correlation/request ID não sensível.

### Offline

- indicar ausência de confirmação do servidor e o horário da última sincronização;
- permitir leitura de cache somente quando a origem e a defasagem forem explícitas;
- bloquear criação de tarefa, aprovação/rejeição e ações em dispositivo;
- nunca enfileirar silenciosamente uma aprovação;
- manter rascunho textual local apenas se não incluir secret e houver política de descarte;
- ao reconectar, atualizar snapshot persistido antes de retomar o stream em tempo real.

### Session expired

É acionado por token expirado sem renovação válida, refresh negado/revogado, issuer/sessão incompatível ou resposta `401` confirmada.

- interromper novas chamadas protegidas e reconexões autenticadas;
- remover material de sessão que não possa mais ser usado;
- não limpar dados do servidor nem transformar logout em exclusão de conta;
- preservar apenas contexto não sensível necessário para explicar a transição;
- voltar ao login com uma mensagem simples;
- depois da reautenticação, revalidar `/v1/me` e workspaces;
- não reenviar automaticamente mutação de efeito desconhecido.

## Mutações e ações sensíveis

- `Submitting` desabilita repetição da mesma intenção, mas não precisa bloquear navegação irrelevante.
- Criação de tarefa e decisão de aprovação usam idempotência definida pelo backend.
- Se a resposta se perder, a UI consulta o recurso pelo identificador/idempotency key antes de retry.
- Aprovação mostra `pending`, `approved`, `rejected`, `expired` ou `superseded` conforme estado persistido; o cliente não deriva decisão apenas da animação/evento.
- Ações otimistas não são usadas para aprovação, autorização, execução em máquina ou resultado final.

## Estados mínimos por tela

| Tela | Loading | Empty | Error | Offline | Session expired |
|---|---:|---:|---:|---:|---:|
| Login | descoberta/configuração | não aplicável | cancelamento/protocolo/provedor | sem login novo | não aplicável |
| Workspaces | inicial/refresh | nenhum vínculo | `401/403/rede` | cache somente leitura | voltar ao login |
| Dashboard | inicial/refresh | módulos individualmente vazios | parcial ou total | snapshot stale | voltar ao login |
| Tarefas | inicial/mais páginas | nenhuma/sem filtro | parcial ou total | lista stale | voltar ao login |
| Nova tarefa | carregar projetos/enviar | nenhum projeto permitido | validação/conflito/rede | não enviar | preservar rascunho seguro e autenticar |
| Detalhe/timeline | snapshot/reconexão | tarefa sem eventos | removida/negada/rede | snapshot stale, stream parado | voltar ao login |
| Aprovação | carregar/enviar decisão | nenhuma pendente | conflito/expirada/negada | somente leitura | não reenviar decisão |
| Resultado | carregar artefatos | sem artefato | removido/negado/rede | cache sanitizado | voltar ao login |

## Acessibilidade e teste

- Estado não depende somente de cor, ícone ou animação.
- Mudanças assíncronas importantes são anunciadas sem repetir continuamente.
- Foco retorna a um ponto previsível após erro, retry e reautenticação.
- Testes de ViewModel cobrem cada estado e transições de precedência.
- Testes instrumentados cobrem rotação/recriação, retry, expiração durante mutação e reconexão.
- Testes E2E cobrem offline antes/depois do envio de aprovação e criação de tarefa.

## Revisão humana pendente

Este contrato não escolhe cor, fonte, espaçamento, shape, motion, ilustração ou densidade. Todos os **tokens visuais**, a decisão **`dark-only` versus `dark-first`** e a apresentação final em tela pequena exigem revisão humana de design e acessibilidade.

