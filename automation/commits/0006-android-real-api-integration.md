# 0006 — Android consome a API real (Retrofit/OkHttp/WebSocket), remove mocks do runtime

Data: 2026-08-27
Status: pendente

## Resumo

Os 4 repositórios do Android (`Task`, `Agent`, `Machine`, `Approval`) passam a falar com o `api-go`
real via Retrofit/OkHttp REST e WebSocket autenticado/reconectável, em vez dos `Fake*` em memória.
Interceptor Bearer + fronteira de token em memória (login/sessão persistente ainda pendente — ver
`CLAUDE.md`). Home, lista de tarefas e detalhe da tarefa ganham estados de loading/vazio/erro reais
(`RemoteStatePanel`). Os `Fake*` migram de `main/` para `test/` — mesmo pacote, deixam de fazer parte
do grafo do Hilt, e os testes existentes (`HomeViewModelTest`, `TaskListViewModelTest`) não mudam.

## Arquivos tocados

- `apps/android/app/src/main/kotlin/com/workcontrol/app/data/remote/` (novo: `ApiCallExecutor.kt`,
  `BearerAuthInterceptor.kt`, `NetworkModule.kt`, `RemoteRepositories.kt`, `TaskEventSource.kt`,
  `WorkControlApi.kt`, `dto/ApiDtos.kt`)
- `apps/android/app/src/main/kotlin/com/workcontrol/app/data/auth/AccessTokenProvider.kt` (novo)
- `apps/android/app/src/main/kotlin/com/workcontrol/app/data/di/RepositoryModule.kt` — `@Binds` trocado
  de `Fake*` para `Remote*`
- `apps/android/app/src/main/kotlin/com/workcontrol/app/domain/error/WorkControlException.kt` (novo)
- `apps/android/app/src/main/kotlin/com/workcontrol/app/core/components/RemoteStatePanel.kt` (novo)
- `apps/android/app/src/test/kotlin/com/workcontrol/app/data/fake/` (movido de `main/`:
  `FakeData.kt`, `FakeRepositories.kt`)
- `apps/android/app/src/test/kotlin/com/workcontrol/app/data/remote/` (novo: `ApiCallExecutorTest.kt`,
  `BearerAuthInterceptorTest.kt`, `RemoteRepositoriesTest.kt`, `TaskEventSourceTest.kt`)
- `docs/adr/0003-android-retrofit-cloudflare-edge.md`

## Mensagem de commit sugerida

```
feat(android): integra API real via Retrofit/OkHttp/WebSocket, remove mocks do runtime

Os 4 repositórios passam a falar com o api-go real (REST + WS
reconectável) em vez de Fake* em memória. Interceptor Bearer com
fronteira de token em memória (login/sessão persistente ainda
pendente). Home/Tarefas/Detalhe ganham loading/vazio/erro reais.
Fake* migram para src/test, mesmo pacote — testes existentes intactos.
```
