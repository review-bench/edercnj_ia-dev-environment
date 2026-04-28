# Prompt: Geração de Épico e Histórias — ia-dev-environment Feature Creation Chain Refactor

> **Instrução de uso**: Execute `/x-epic-decompose docs/specs/SPEC-feature-creation-chain-refactor-v1.md --no-jira`.

---

## Sistema

**Projeto**: `ia-dev-environment` — CLI generator de ambientes de desenvolvimento assistidos por IA.

**Versão base analisada**: `develop` pós-EPIC-0062 (v3→v4 folder finalization).

**Objetivo desta especificação**: Refatorar a chain de criação de feature/épico/story para tornar a entrada do pipeline mais natural (texto livre → spec estruturada via `x-feature-ideate`), promover `x-epic-decompose` ao papel claro de "criar a feature inteira" sob o nome `x-feature-create` (com worktree real e PR para `epic/XXXX`), rebaixar `x-epic-create`, `x-epic-map` e `x-story-create` à categoria interna (chamadas exclusivamente pelo orquestrador), e habilitar branch `docs/<epic-id>-<slug>` como veículo padrão da chain de criação. Paralelamente, formalizar Rule 19 para permitir hard-cut em renames que mudam papel semântico (não apenas nome).

**Princípio central de todas as histórias**: A chain de criação hoje termina com commit direto em `epic/XXXX` sem PR (perdendo audit trail de revisão), opera fora de worktree (acoplando o checkout principal), e expõe 3 entry-points públicos sobrepostos (`x-epic-create`, `x-epic-decompose`, `x-epic-orchestrate`) que confundem o operador sobre quem cria o quê. A refatoração: (a) introduz `x-feature-ideate` como ponto de entrada para texto livre, (b) consolida criação de epic+stories+map sob `x-feature-create` (renomeação de `x-epic-decompose`) com worktree próprio e PR auto-merged contra `epic/XXXX`, (c) rebaixa as 3 skills sub-papéis a `x-internal-*`, (d) preserva `x-epic-orchestrate` como skill de planning multi-agente (sem criar épico) com nota explicativa.

---

## Escopo do Épico

### Contexto de negócio

Auditoria das 10 skills de criação revela três gaps técnicos:

1. **Nenhuma skill da chain de criação abre PR** — todas commitam direto em `epic/XXXX` via `x-planning-commit`. O único PR existente é o gate manual `epic/XXXX → develop` no fim do épico, o que mistura PRs de planning e de implementação na mesma branch e impede revisão isolada da decomposição.

2. **Worktree é apenas advisory** — as 7 skills de planning invocam `x-git-worktree detect-context` em Step P1, mas nenhuma cria worktree próprio. A chain de criação opera direto no checkout principal, ocupando-o por toda a duração e impedindo o operador de continuar trabalhando em paralelo.

3. **3 entry-points públicos com responsabilidades confundidas** — `x-epic-create` (cria apenas epic-XXXX.md), `x-epic-decompose` (cria epic + N stories + map em pass único), `x-epic-orchestrate` (faz planning multi-agente das stories já criadas). Operadores invocam o nome errado por confusão (relato direto do mantenedor em 2026-04-28 confundindo `x-epic-orchestrate` com responsável por criar o épico).

Adicionalmente, **não existe ponto de entrada para "texto livre → spec"**. O operador precisa redigir manualmente um SPEC formatado em markdown antes de invocar a chain. Isso eleva a barreira de entrada para uso casual e bloqueia ideação assistida.

### Dimensões de melhoria

1. **Skill nova `x-feature-ideate`** — pública, recebe parágrafo livre como argumento, gera `docs/specs/SPEC-<slug>-v1.md` estruturado (RA9 v2 minimal), opera em worktree `feature-ideation-<slug>`, branch `docs/feature-<slug>`, PR `→ develop` com label `docs`. Composição explícita: termina em spec.md sem encadear `x-feature-create` automaticamente — o operador revisa/aprova antes de prosseguir.

2. **Renomeação `x-epic-decompose` → `x-feature-create`** — promove a skill para "criar feature inteira". Adiciona criação de worktree real `feature-XXXX-<slug>`, branch `docs/<epic-id>-<slug>` (base `epic/XXXX`), PR `→ epic/XXXX` `--auto-merge merge` com label `docs`. Mantém o "1 commit consolidado" e a integração Jira opcional.

3. **Renomeação `x-epic-create` → `x-internal-epic-create`** — rebaixa para skill interna (visibility: internal, user-invocable: false, body marker 🔒). Remove Phases P1/P2/P4/P5 (orquestrador é o caller único). Invocada exclusivamente por `x-feature-create`.

4. **Renomeação `x-epic-map` → `x-internal-epic-map`** — idem.

5. **Renomeação `x-story-create` → `x-internal-story-create`** — idem.

6. **Atualização Rule 09** — adiciona tipo `docs/` ao branching model com 2 padrões: `docs/<epic-id>-<slug>` (chain de criação dentro de épico, base `epic/XXXX`) e `docs/feature-<slug>` (ideação pré-épico, base `develop`). Define naming, máximo 100 chars, lifecycle preservado por `x-git-cleanup-branches` enquanto PR aberto.

7. **Atualização Rule 14 (Worktree Lifecycle)** — adiciona padrão `feature-XXXX-<slug>` (worktree de feature-creation, base `epic/XXXX`) e `feature-ideation-<slug>` (worktree de ideação, base `develop`). Define que orquestrador cria, sub-skills herdam via §3 (idempotent reentrancy).

8. **Atualização Rule 19 (Backward Compatibility)** — adiciona cláusula permitindo hard-cut sem deprecation window quando o rename muda o papel semântico da skill (public→internal, fusão, ou troca de subject taxonomy), não apenas o nome. Justificativa documentada em CHANGELOG. Estabelece exatamente os 4 casos hard-cut autorizados nesta epic.

9. **Atualização Rule 22 (Skill Visibility)** — adiciona exemplos da nova chain (`x-internal-epic-create`, `x-internal-epic-map`, `x-internal-story-create`) à seção de exemplos válidos. Sem mudança normativa.

10. **Atualização `x-epic-orchestrate`** — adiciona nota visível no SKILL.md esclarecendo que NÃO cria épico nem story (responsabilidade de `x-feature-create`); permanece responsável por planning multi-agente das stories já criadas. Sem mudança comportamental.

11. **Audit scripts** — `audit-skill-visibility.sh` reconhece os 3 novos internals; `audit-epic-branches.sh` reconhece tipo `docs/`; `x-git-cleanup-branches` preserva `docs/*` enquanto PR aberto.

12. **Templates e docs** — `_TEMPLATE-SKILL.md` ganha exemplo da chain; `_TEMPLATE-EPIC.md` menciona branch `docs/<epic-id>-<slug>`; CHANGELOG com entrada `[3.X.0] - 2026-04-XX`; `.claude/README.md` regenerado refletindo nova taxonomia.

13. **Smoke test E2E** — novo Java test `Epic0065SmokeTest` que valida ponta-a-ponta: parágrafo de input → `x-feature-ideate` → spec gerada → `x-feature-create` → epic + 2 stories + map em `epic/0065smoke` + PR contra `epic/0065smoke` → 1 story planejada via `x-epic-orchestrate`.

### Métricas de sucesso

| Métrica | Antes | Target |
|---|---|---|
| Entry-points públicos para criar feature | 3 (sobrepostos) | 2 (claros: ideate + create) |
| Skills internas dedicadas à chain de criação | 0 | 3 (epic-create, epic-map, story-create) |
| Skills que criam PR para `epic/XXXX` na chain de criação | 0 | 1 (`x-feature-create`) |
| Tipos de branch suportados na Rule 09 | 5 (`epic/`, `feature/`, `release/`, `hotfix/`, `fix/`) | 6 (+ `docs/`) |
| Padrões de worktree na Rule 14 | 3 (task/story/epic) | 5 (+ feature-XXXX, feature-ideation) |
| Tempo do operador desde "ter ideia" até "ter spec.md" | ~30 min (manual) | <5 min (`x-feature-ideate`) |
| Auditabilidade da criação | commit-only em epic/XXXX | PR mergeado em epic/XXXX (review trail) |
| Audit `audit-skill-visibility.sh` | passa | passa (sem regressão) |
| `mvn verify` (incluindo `Epic0065SmokeTest`) | n/a | passa |

---

## Regras de Negócio Transversais (Cross-Cutting Rules)

**RULE-001 — Worktree único por chain de criação**: Toda invocação da chain de criação (`x-feature-create` e suas sub-skills internas) opera em **um único worktree** criado pelo orquestrador `x-feature-create` em `.claude/worktrees/feature-XXXX-<slug>/`. As sub-skills (`x-internal-epic-create`, `x-internal-epic-map`, `x-internal-story-create`) herdam via Rule 14 §3 (idempotent reentrancy). `x-feature-ideate` opera em worktree separado `feature-ideation-<slug>` por trabalhar fora de qualquer épico (épico ainda não existe na fase de ideação).

**RULE-002 — Branch da chain de criação**: A chain de criação opera em `docs/<epic-id>-<slug>`, base = `epic/XXXX` (criada antes pelo `x-internal-epic-branch-ensure`). PR alvo = `epic/XXXX` com `--auto-merge merge` e label `docs`. `x-feature-ideate` opera em `docs/feature-<slug>` (sem epic-id pois épico ainda não existe), PR alvo `develop`.

**RULE-003 — Naming**: Skills internas usam prefixo `x-internal-` exato (Rule 22). Skills públicas seguem `x-{subject}-{action}` (EPIC-0036). Os nomes desta epic são: `x-feature-ideate`, `x-feature-create`, `x-internal-epic-create`, `x-internal-epic-map`, `x-internal-story-create`.

**RULE-004 — Hard-cut autorizado**: Os 4 nomes antigos (`x-epic-decompose`, `x-epic-create`, `x-epic-map`, `x-story-create`) deixam de existir nesta epic, sem alias temporário. Rule 19 ganha cláusula nova permitindo hard-cut quando a refatoração muda o papel semântico (public→internal, fusão, ou troca de subject taxonomy), não apenas o nome.

**RULE-005 — Composição explícita ideate→create**: `x-feature-ideate` gera o spec.md + abre PR `docs/feature-<slug> → develop` e termina. NÃO chama `x-feature-create` automaticamente. O operador revisa/aprova/edita o spec, depois invoca `/x-feature-create <spec.md>` manualmente. Decisor humano no meio do pipeline.

**RULE-006 — `x-epic-orchestrate` permanece sem mudança comportamental**: A skill continua pública e responsável por planning multi-agente das stories já criadas (chama `x-story-plan` por story em ordem de fase). Apenas o SKILL.md ganha nota visível esclarecendo que NÃO cria épico nem story. Operadores que invocarem por engano recebem orientação para `x-feature-create`.

**RULE-007 — Skills de planning standalone fora do escopo**: `x-story-plan`, `x-arch-plan`, `x-test-plan`, `x-task-plan` mantêm comportamento atual (commit direto em `epic/XXXX` via `x-planning-commit`/`x-git-commit`, sem worktree próprio e sem PR adicional). Mudança eventual delas é fora desta epic.

**RULE-008 — Tipo `docs/` no branching model**: Rule 09 ganha 2 padrões: `docs/<epic-id>-<slug>` (chain de criação dentro de épico, base `epic/XXXX`) e `docs/feature-<slug>` (ideação pré-épico, base `develop`). Cleanup excluído por `x-git-cleanup-branches` enquanto PR aberto.

**RULE-009 — Audit scripts atualizadas**: `audit-skill-visibility.sh` reconhece os 3 novos internals; `audit-epic-branches.sh` reconhece tipo `docs/` como válido em conjunto com `epic/`; `x-git-cleanup-branches` preserva `docs/*` enquanto PR aberto.

**RULE-010 — DoR/DoD global**: Cada story que mexe em SKILL.md regenera `.claude/skills/` via `mvn process-resources && java -cp ... GoldenFileRegenerator` e revalida com `audit-skill-visibility.sh --self-check`, `audit-epic-branches.sh`, `audit-flow-version.sh`. Cada story tem teste Java associado quando toca código gerador (assemblers/parsers).

---

## Histórias

### Layer 0 — Foundation (regras + audits)

**STORY-0065-0001 — Atualização das Rules e audits**: Edita `java/src/main/resources/targets/claude/rules/09-branching-model.md` (adiciona tipo `docs/` com 2 padrões), `java/src/main/resources/targets/claude/rules/14-project-scope.md` (Worktree Lifecycle: padrões `feature-XXXX-<slug>` e `feature-ideation-<slug>`), `java/src/main/resources/targets/claude/rules/19-backward-compatibility.md` (cláusula hard-cut autorizada), `java/src/main/resources/targets/claude/rules/22-skill-visibility.md` (exemplos da chain). Atualiza `scripts/audit-skill-visibility.sh`, `scripts/audit-epic-branches.sh` e (se necessário) `scripts/audit-flow-version.sh` para reconhecer `docs/` e os 3 novos internals. Regenera `.claude/rules/`. **Sem dependências.**

### Layer 1 — Core skills (entradas públicas)

**STORY-0065-0002 — Skill nova `x-feature-ideate`**: Cria nova skill pública em `java/src/main/resources/targets/claude/skills/core/plan/x-feature-ideate/SKILL.md`. Frontmatter: `name: x-feature-ideate`, `model: opus` (deep-reasoning para transformar prosa em spec estruturada), `user-invocable: true`, `argument-hint: "<paragraph or text-file>"`. Phases: P0 worktree-create `feature-ideation-<slug>` + branch `docs/feature-<slug>`; Phase 1 análise da prosa (rules, stories preliminares, layers); Phase 2 escrita de `docs/specs/SPEC-<slug>-v1.md` (RA9 v2 minimal: Sistema/Escopo/Rules/Histórias); Phase 3 commit + push + PR `→ develop` (label `docs`); Phase 4 report com link do PR e instrução para invocar `/x-feature-create`. Subagent opus único para análise. **Depende: STORY-0065-0001.**

**STORY-0065-0003 — Renomeação `x-epic-decompose` → `x-feature-create`**: Renomeia diretório `java/src/main/resources/targets/claude/skills/core/plan/x-epic-decompose/` → `x-feature-create/` via `git mv`. Atualiza frontmatter (`name: x-feature-create`, `argument-hint` mantém `[SPEC-FILE-PATH]`). Adiciona Phase P1.5: criação do worktree `feature-XXXX-<slug>` via `x-git-worktree create --branch docs/<epic-id>-<slug> --base epic/XXXX --id feature-XXXX-<slug>`. Substitui Phase P5 (push em epic/XXXX) por Phase P5/P6: push da branch `docs/<epic-id>-<slug>` + criação de PR `→ epic/XXXX` via `x-pr-create --target-branch epic/XXXX --auto-merge merge --label docs`. Mantém commit consolidado e Jira integration. Atualiza inline references aos sub-skills renomeados (epic-create, story-create, epic-map → x-internal-*). Hard-cut: nome antigo é removido. **Depende: STORY-0065-0001.**

### Layer 2 — Sub-skills (rebaixamento a internas)

**STORY-0065-0004 — `x-internal-epic-create`**: Renomeia `java/src/main/resources/targets/claude/skills/core/plan/x-epic-create/` → `internal/epic/x-internal-epic-create/` via `git mv`. Frontmatter: `visibility: internal`, `user-invocable: false`, body marker 🔒. Remove Phases P1 (worktree detect), P2 (epic-branch-ensure), P4 (planning-commit), P5 (push) — orquestrador `x-feature-create` é o caller único e dono dessas operações. Hard-cut: nome antigo removido. **Depende: STORY-0065-0003.**

**STORY-0065-0005 — `x-internal-epic-map`**: Renomeia `java/src/main/resources/targets/claude/skills/core/plan/x-epic-map/` → `internal/epic/x-internal-epic-map/`. Idem ao 0004 (mesmo padrão de rebaixamento). **Depende: STORY-0065-0003.**

**STORY-0065-0006 — `x-internal-story-create`**: Renomeia `java/src/main/resources/targets/claude/skills/core/plan/x-story-create/` → `internal/story/x-internal-story-create/`. Idem ao 0004. Atualiza referências internas em `x-feature-create`. **Depende: STORY-0065-0003.**

### Layer 3 — Disambiguação e ferramentas

**STORY-0065-0007 — Atualização `x-epic-orchestrate`**: Edita `java/src/main/resources/targets/claude/skills/core/plan/x-epic-orchestrate/SKILL.md` adicionando bloco visível no topo do "When to Use": "Esta skill **não cria** épico nem stories. Para criar a feature inteira a partir de um spec, use `/x-feature-create`. Esta skill apenas orquestra **planning multi-agente** das stories já criadas." Sem mudança comportamental. **Depende: STORY-0065-0003.**

**STORY-0065-0008 — Audit scripts e cleanup branches**: Atualiza `scripts/audit-skill-visibility.sh` para reconhecer os 3 novos internals (não falhar com `SKILL_VISIBILITY_VIOLATION`). Atualiza `scripts/audit-epic-branches.sh` para reconhecer tipo `docs/` (extrai epic-id do nome `docs/<XXXX>-...` e valida contra existência de `epic/<XXXX>`). Atualiza `x-git-cleanup-branches` (Bash) para preservar `docs/*` quando PR aberto via `gh pr list --head <branch> --state open`. **Depende: STORY-0065-0001, STORY-0065-0004, STORY-0065-0005, STORY-0065-0006.**

### Layer 4 — Templates e docs

**STORY-0065-0009 — Templates, CHANGELOG, README**: Atualiza `java/src/main/resources/shared/templates/_TEMPLATE-SKILL.md` (exemplo da chain de criação na seção Integration Notes); `java/src/main/resources/shared/templates/_TEMPLATE-EPIC.md` (menciona `docs/<epic-id>-<slug>` como branch padrão de planning); `CHANGELOG.md` com entrada `[3.X.0] - 2026-04-XX` documentando os hard-cuts e os novos nomes; regenera `.claude/README.md` via `mvn process-resources` para refletir o catálogo atualizado. **Depende: STORY-0065-0002, STORY-0065-0003, STORY-0065-0007.**

### Layer 5 — Verificação E2E

**STORY-0065-0010 — Smoke test E2E**: Cria `java/src/test/java/dev/iadev/Epic0065SmokeTest.java` que valida ponta-a-ponta: (a) prosa fixa "Implementar feature de exportação CSV..." é input para `x-feature-ideate`; (b) spec é gerado em `docs/specs/SPEC-csv-export-v1.md` em branch `docs/feature-csv-export`; (c) PR é criado contra `develop`; (d) usando esse spec, `x-feature-create` cria `epic/0065smoke` com epic-0065smoke.md + 2 stories + IMPLEMENTATION-MAP em branch `docs/0065smoke-csv-export` e PR contra `epic/0065smoke`; (e) `x-epic-orchestrate` invocado em uma das stories produz o consolidado de planning. Usa fixtures isoladas (não polui plans/). **Depende: TODAS as outras stories.**

---

## Definições de Qualidade

### DoR Global
- Spec aprovada
- Decisões de design confirmadas (este documento)
- Branch `epic/0065` criada
- IMPLEMENTATION-MAP gerado pela própria skill `x-epic-decompose`

### DoD Global
- Audits verdes: `audit-skill-visibility.sh`, `audit-epic-branches.sh`, `audit-flow-version.sh`, `audit-task-hierarchy.sh`, `audit-phase-gates.sh`, `audit-execution-integrity.sh`, `audit-model-selection.sh`
- `mvn verify` verde (incluindo `Epic0065SmokeTest`)
- `.claude/skills/` regenerado via `GoldenFileRegenerator`; diff somente nas skills do épico
- CHANGELOG entry `[3.X.0] - 2026-04-XX` documentando hard-cuts
- Cada story segue TDD (Double-Loop quando aplicável): testes-primeiro para CLI/parsers, refactor explícito após GREEN
- 4 PRs `docs/0065-*` mergeados em `epic/0065` + 1 PR final manual `epic/0065 → develop`

---

## Riscos / Considerações

| Risco | Mitigação |
|---|---|
| Hard-cut quebra orquestradores externos que invocam nomes antigos por string | STORY-0065-0001 atualiza Rule 19 ANTES de qualquer rename; STORY-0065-0008 grep cruzado em todo o repo + audit |
| Worktree-by-orchestrator falha em re-entrancy (operador já em worktree) | Reusar Rule 14 §3 idempotent reentrancy, padrão idêntico a `x-task-implement --worktree` |
| `audit-skill-visibility.sh` falha em CI durante a transição (entre 0001 e 0008) | DAG força 0001 antes de 0003-0006; CI gate por fase |
| Confusão sobre `epic/XXXX` vs `docs/XXXX-<slug>` em operadores | STORY-0065-0007 + 0009 documentam; Rule 09 ganha diagrama atualizado |
| `x-feature-ideate` produz spec de baixa qualidade para inputs vagos | Subagent opus + validação RA9 v2 mínima (Sistema/Escopo/Rules/Histórias presentes); operador revisa antes de invocar `x-feature-create` |
| Renames de pasta quebram git history visualmente | `git mv` preserva history; STORY-0065-0008 inclui `git log --follow` smoke check |
| 4 PRs simultâneos em `epic/0065` exaurem CI | Rule 14 + Rule 21 já preveem; CI scaling não é responsabilidade desta epic |

---

## Telemetria e Observabilidade

Padrão TELEMETRY: phase.start/phase.end já estabelecido (EPIC-0040). `x-feature-ideate` e `x-feature-create` ganham markers em todas as phases. Eventos NDJSON em `plans/epic-0065/telemetry/events.ndjson`. Análise via `/x-telemetry-analyze --epic EPIC-0065`.

---

## Versão e Compatibilidade

- Compatível com `flowVersion=4` (EPIC-0062).
- Não altera contratos `execution-state.json`.
- Não introduz novos campos obrigatórios.
- Hard-cut em 4 nomes (cláusula nova de Rule 19).
- Branch `docs/` é tipo novo, não-breaking.
