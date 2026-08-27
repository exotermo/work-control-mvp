# 0001 — Scaffold Android nativo (Kotlin/Compose) a partir do protótipo Figma

Data: 2026-08-20
Status: pendente

## Resumo

Criado o projeto Android nativo em `apps/android/` (Gradle wrapper, Kotlin 2.0.21, Jetpack Compose,
Clean Architecture + MVVM/MVI, Hilt via KSP), portando fielmente o design system e 3 telas completas
do `Work Control Mobile Prototype` (Início, Tarefas, Detalhe da Tarefa), com as 11 telas restantes
como placeholders navegáveis. Build de debug (`assembleDebug`) e suíte de testes unitários
(`testDebugUnitTest`, 5/5 passando) verificados de ponta a ponta neste ambiente, incluindo instalação
e navegação manual em um emulador Android real (screenshots em anexo na conversa).

Detalhes completos da decisão de arquitetura e do que falta portar: ver o relatório "Diário de Bordo"
(Artifact publicado na conversa) e os comentários de cada `PlaceholderScreen` em
`app/src/main/kotlin/com/workcontrol/app/feature/*`.

## Arquivos tocados

- `apps/android/` — projeto Gradle completo (novo): `settings.gradle.kts`, `build.gradle.kts`,
  `gradle/libs.versions.toml`, `gradlew` + wrapper, `app/build.gradle.kts`, `app/src/main/AndroidManifest.xml`
- `apps/android/app/src/main/kotlin/com/workcontrol/app/` — todo o código-fonte (design system,
  componentes compartilhados, domínio, dados fake, navegação, features home/tasks/taskdetail completas
  + 9 features placeholder)
- `apps/android/app/src/test/kotlin/com/workcontrol/app/` — testes unitários (JUnit4 + MockK + Turbine)
- `apps/android/app/src/main/res/` — strings, cores, tema, ícone adaptativo
- `.gitignore` — adicionadas entradas Android (`local.properties`, `.cxx/`, `*.jks`, `*.keystore`, `captures/`)
- `docs/pgp/README.md`, `docs/sdd/README.md`, `automation/commits/README.md` — novos índices de documentação

## Mensagem de commit sugerida

```
feat(android): scaffold do app nativo Kotlin/Compose a partir do protótipo Figma

Clean Architecture + MVVM/MVI, Hilt/KSP, Navigation Compose type-safe.
Home, Tarefas e Detalhe da Tarefa portados por completo com dados fake
reativos; demais 11 telas como placeholders navegáveis. Build debug e
testes unitários (5/5) verificados neste ambiente.
```
