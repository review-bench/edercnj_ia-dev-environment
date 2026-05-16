# Instruções para Revisões do GitHub Copilot

> Este arquivo é lido automaticamente pelo GitHub Copilot ao fazer code review
> em pull requests deste repositório (`ia-dev-environment`). Ele complementa o
> contexto carregado de `.claude/rules/` e `CLAUDE.md`.

## Idioma das Revisões — OBRIGATÓRIO

**Você DEVE responder exclusivamente em português brasileiro.**
Nunca use inglês em comentários, descrições de issues ou sugestões.
Identificadores de código (nomes de arquivos, comandos, variáveis), termos
técnicos consagrados (commit, merge, hash, branch), palavras-chave de
linguagem e nomes próprios permanecem em sua forma original.

Exemplo correto:
> "O bloco de skip em `preflight.sh` referencia um gate removido. Considere
> remover as linhas órfãs."

Exemplo incorreto:
> "The skip block in `preflight.sh` references a removed gate. Consider
> removing the orphan lines."

Acentuação completa é obrigatória — nunca substitua caracteres acentuados
por equivalentes ASCII (`não`, não `nao`; `é`, não `e`).

## Identidade do Projeto

- **Repositório:** `ia-dev-environment` — o **store fonte-da-verdade** de um
  ambiente Claude Code completo: rules, skills, agents, knowledge packs,
  templates e `settings.json`.
- **Conteúdo canônico:** vive sob `resources/` (versionado).
- **Distribuição:** `bin/install-claude-resources.sh` copia `resources/`
  verbatim para o `.claude/` de um projeto-alvo, mais um `CLAUDE.md` stub.
  Não há build, não há Java, não há templating — tokens `{{PLACEHOLDER}}`
  são resolvidos em runtime pelo próprio Claude.
- **Stack:** Markdown (skills/rules/knowledge/agents/templates) + Bash
  (scripts de audit e o instalador). Sem código de aplicação compilado.
- **Nota sobre identidade:** o placeholder `my-java-cli` em
  `resources/rules/01-project-identity.md` é apenas marcador de template e
  NÃO se aplica a este repositório. Use sempre `ia-dev-environment`.

## Foco da Revisão (em ordem de prioridade)

1. **Segurança em shell — Rule 06 + Rule 12.** Sinalize SEMPRE:
   - Interpolação de variável não citada em comando (`rm $x` → `rm "$x"`).
   - `eval` sobre conteúdo derivado de input não validado.
   - Credenciais/tokens hardcoded em scripts ou em `settings.json`.
   - `curl ... | bash` sem verificação.
   - Path traversal: caminhos derivados de input sem normalização/contenção.
   - Ausência de `set -euo pipefail` em scripts novos não triviais.

2. **Coerência de conteúdo (skills/rules/knowledge).** Sinalize:
   - Links internos quebrados entre skills, rules e knowledge packs.
   - Referência a caminho inexistente (ex.: `java/src/...`, `target/`,
     `pom.xml`, `ia-dev-env generate` — resquícios do build Java removido).
   - Frontmatter YAML inválido ou `requires-capabilities` ausente onde a
     convenção (Rule 28) exige.
   - Skill que escreve artefato sem o passo de commit correspondente.

3. **Padrões de shell — Rule 03.** Diretrizes:
   - Scripts devem passar `shellcheck` e `bash -n`.
   - Funções coesas; sem lógica duplicada entre scripts de audit.
   - Sem `echo`/`cat` para mutar arquivo quando uma ferramenta dedicada serve.
   - Largura de linha ≤ 120 caracteres em prosa de documentação.

4. **Convenção de instalação.** Sinalize:
   - Edição direta de `.claude/` (é install local gitignored — editar
     `resources/` e reinstalar).
   - Divergência entre `resources/` e o que o instalador copia.

5. **Conventional Commits + branch naming — Rules 08, 09.**
   - Tipos: `feat`, `fix`, `chore`, `docs`, `refactor`, `test`, `perf`.
   - Quebra de contrato exige `feat!:` / `fix!:` ou `BREAKING CHANGE:`.
   - Branches: `feature/*`, `fix/*`, `chore/*`, `epic/*`, `release/*`,
     `hotfix/*`. Lowercase com hífen, sem underscore/camelCase.

## Como Comentar

- **Seja específico:** aponte arquivo e linha.
- **Cite a regra:** "Rule 03", "Rule 06", "CWE-78" — facilita rastreabilidade.
- **Sugira correção concreta:** mostre o snippet ajustado quando possível.
- **Severidade implícita:** segurança e links quebrados = crítico; estilo = sugestão.
- **Tom direto, técnico, sem rodeios.** Nada de saudações ou agradecimentos.

## O Que NÃO Fazer

- **Não comente preferências estilísticas** já cobertas por linter
  (espaçamento, ordenação).
- **Não duplique feedback do CI** — assuma que checagens automatizadas reportam.
- **Não sugira refatorações fora do diff** do PR. Mantenha o escopo.
- **Não peça mudanças em `ai/**`, ADRs ou `governance/baselines/*.txt`** —
  são histórico/imutáveis por contrato.
- **Não bloqueie PRs por nits.** Reserve "request changes" para violações
  de Rules 03–06, 12 ou conteúdo quebrado real.

## Caminhos com Tratamento Especial

- **`resources/`** — fonte-da-verdade. Toda mudança de skill/rule/agent/
  knowledge/template começa aqui.
- **`resources/scripts/audit-*.sh`** — scripts de audit distribuídos.
  Contrato (Rule 26): prefixo obrigatório `audit-`, exit codes padronizados
  (0/1/2/3), suporte a `--self-check`.
- **`scripts/*.sh`, `.githooks/*`, `bin/*.sh`** — tooling repo-local. Devem
  passar `shellcheck`/`bash -n`; sem invocação de `mvn`/`java` (build Java
  foi removido).
- **`.claude/`** — install local **gitignored**. Não comente seu conteúdo;
  comente a fonte em `resources/` e o instalador `bin/install-claude-resources.sh`.
- **`ai/**`, `docs/adr/**`** — histórico imutável; não pedir reescrita.

## Quando uma Sugestão Sua Conflitar com uma Regra do Projeto

A regra do projeto (`.claude/rules/NN-*.md`) **sempre vence**. Se você
identificar conflito entre uma "best practice" genérica e uma regra deste
projeto, mencione a regra explicitamente e proponha alinhamento à regra,
não o contrário.

## Versão

`v2.0` — alinhado à transformação do repositório em store de recursos puro
(remoção do build Java/Maven). Mantém a estrutura de seções da v1.0 para
preservar referências em PRs antigos.
