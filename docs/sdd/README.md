# Software Design Document (SDD)

Índice de tópicos para o(s) Software Design Document deste projeto — o documento que aprofunda
cada decisão de arquitetura além do que cabe no `README.md` raiz e nos ADRs (`docs/adr/`).

## Tópicos

1. **Visão geral e objetivo** — que problema o componente/serviço resolve, em uma página.
2. **Escopo** — o que está dentro e o que está explicitamente fora desta versão.
3. **Stakeholders e público-alvo** — quem consome o serviço/API/tela documentada.
4. **Arquitetura de componentes** — como o componente se encaixa no desenho geral (ligar para `docs/architecture/`).
5. **Modelo de dados** — entidades, relacionamentos, ligar para o schema real quando existir.
6. **Contratos de API / interfaces** — endpoints, mensagens, eventos trocados entre serviços.
7. **Fluxos principais** — os caminhos felizes e os de erro mais importantes, com diagrama de sequência quando ajudar.
8. **Requisitos não-funcionais** — performance, segurança, disponibilidade, limites conhecidos.
9. **Decisões de design e alternativas consideradas** — o porquê, não só o quê (ligar para `docs/adr/` quando virar uma ADR formal).
10. **Estratégia de testes** — o que precisa de cobertura automatizada e o que é validado manualmente.
11. **Riscos e mitigação** — o que pode dar errado e o plano B.
12. **Plano de rollout / migração** — como a mudança chega em produção com segurança.
13. **Questões em aberto** — o que ainda não foi decidido, para não fingir certeza que não existe.

Cada componente relevante (API Go, Orchestrator Java, Device Agent Go, app Android) deve ganhar seu
próprio SDD seguindo esses tópicos conforme o design amadurece — este README é só o índice/modelo.
