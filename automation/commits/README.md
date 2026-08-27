# Commits pendentes

Este monorepo ainda não é um repositório Git inicializado, e o mantenedor alterna entre mais de uma
conta GitHub. Para não gerar commits sob a identidade/credencial errada, cada implementação
bem-sucedida é registrada aqui como um arquivo de log em vez de virar um `git commit` direto.

## Convenção

Um arquivo por implementação: `NNNN-slug.md`, numeração sequencial com 4 dígitos.

Conteúdo mínimo:

```markdown
# NNNN — Título curto

Data: AAAA-MM-DD
Status: pendente | aplicado

## Resumo
O que foi feito e por quê (1-3 frases).

## Arquivos tocados
- caminho/arquivo1
- caminho/arquivo2

## Mensagem de commit sugerida
```
tipo(escopo): descrição curta

Corpo opcional explicando o porquê.
```
```

## Aplicando os commits

Quando estiver logado na conta GitHub correta:

1. `git init` (se ainda não houver repositório) e configure `user.name`/`user.email` para essa conta.
2. Para cada arquivo aqui, em ordem: `git add` os arquivos listados e `git commit` com a mensagem sugerida.
3. Marque o arquivo como `Status: aplicado` (ou apague-o, se preferir não manter histórico duplicado).

Nenhum commit real deve ser criado a partir deste diretório sem confirmação explícita de qual conta/identidade usar.
