# Rule 28 — Capability Frontmatter Contract

> **Related:** Rule 13 (Skill Invocation Protocol), Rule 22 (Skill Visibility), Rule 26 (Audit Gate Lifecycle).
> **Introduced by:** EPIC-0064 (Capability-Driven Composition Refactor).
> **ADR:** ADR-0016.

## Purpose

Estabelece o contrato de **frontmatter universal v3.0** para todo artefato gerado por `ia-dev-env`. Antes do EPIC-0064, frontmatter era usado apenas para metadata identificadora (`name`, `description`, `model`) e injeção parcial (`context-budget`). A composição de `.claude/` era copy-cego, ignorando capabilities declaradas no YAML do projeto.

Rule 28 codifica que **toda decisão de incluir/excluir/parametrizar um artefato passa por frontmatter declarativo**, validado por schema, auditado por CI.

## Scope

Aplica-se a todos os 6 tipos de artefato gerados pelo `ia-dev-env`:

| Artefato | Source-of-truth path | Output path |
| :--- | :--- | :--- |
| Skill | `targets/claude/skills/<category>/<name>/SKILL.md` | `.claude/skills/<name>/SKILL.md` |
| Rule | `targets/claude/rules/core/<NN>-<name>.md` + `rules/fragments/<NN>/*.md` | `.claude/rules/<NN>-<name>.md` |
| Knowledge Pack | `targets/claude/knowledge/<name>/index.md` + `knowledge/<name>/fragments/*.md` | `.claude/knowledge/<name>/index.md` |
| Agent | `targets/claude/agents/<name>.md` | `.claude/agents/<name>.md` |
| Hook | `targets/claude/hooks/<event>/<name>.sh` | `.claude/hooks/<event>/<name>.sh` |
| Template | `shared/templates/_TEMPLATE-<name>.md` | `.claude/templates/_TEMPLATE-<name>.md` |

Internal skills (`x-internal-*`, Rule 22) seguem o mesmo contrato — não há exceção.

## Invariantes (8)

1. **Declaração obrigatória.** Todo artefato fora de `_common/` DEVE declarar `requires-capabilities: [...]` em frontmatter v3.0. Ausência do campo (não vazia, mas literalmente ausente) é build error: `audit-capability-coverage.sh` exit 1.

2. **Schema v3.0 estrito.** Frontmatter validado por `governance/schemas/frontmatter-3.0.json`. Campos extras desconhecidos são tolerados (additionalProperties=true) para extensibilidade futura, mas campos conhecidos têm tipos rígidos. v2 (sem `requires-capabilities`) quebra build.

3. **Capability ID válido.** `requires-capabilities`, `requires-any`, `excludes-capabilities` referenciam apenas IDs declarados em `capabilities/<category>/<id>.yaml`. Glob `category.subcategory.*` e `category.subcategory.**` permitidos. Capability inexistente = `audit-capability-graph.sh` exit 3 (`UnknownCapability`).

4. **Excludes simétrico.** Se A declara `excludes-capabilities: [B]`, então B DEVE declarar `excludes-capabilities: [A]`. Assimetria = `audit-capability-graph.sh` exit 2 (`AsymmetricMutex`).

5. **Composition-priority bounded.** Se declarado, `composition-priority ∈ [0, 100]`. Default 50. Maior vence quando múltiplos artefatos colidem no mesmo path.

6. **Fragment ≠ artifact standalone.** Arquivos sob `<artifact-parent>/fragments/` DEVEM declarar `fragment-slot: { slot, fragment-id, fragment-order }`. Sem ele, scanner trata como artifact standalone — pode gerar arquivo de output indevido. Fragment com `fragment-slot` NUNCA gera arquivo próprio.

7. **Slot ↔ Fragment coerência.** Para todo `fragment-slots: [{ slot: X }]` declarado em artifact-pai, deve existir pelo menos uma referência `{{ slot: X }}` ou `{{ #each fragments.X }}` no body do artifact-pai. `audit-fragment-coherence.sh` valida.

8. **Sem retrocompatibilidade.** Schema versão 3.0 é obrigatório a partir do merge de EPIC-0064. Não há dual-mode v2/v3. CHANGELOG marca `[Breaking]` + bump major.

## Examples

### (a) Skill condicional
```yaml
---
name: x-review-db
description: Database specialist review
visibility: public
model: sonnet
allowed-tools: Read, Grep, Glob, Bash
requires-capabilities: [data.database.*]
parameters-from: [data.database.name, data.database.version]
fragment-slot: { slot: review-specialist, fragment-id: db, fragment-order: 30 }
---
# Skill: Database Review
Reviews migrations, indexes, and query patterns for {{ capabilities.data.database.name }} {{ capabilities.data.database.version }}.
```

### (b) Rule fragmentada
```yaml
# rules/core/09-data-management.md (universal core)
---
name: rule-09
requires-capabilities: []
fragment-slots: [{ slot: data-migration, ordering: fragment-order }, { slot: data-cache, ordering: fragment-order }]
---
# Rule 09 — Princípios de Data Management
... idempotência, transações, naming ...
{{ slot: data-migration }}
{{ slot: data-cache }}
```

```yaml
# rules/fragments/09/data-migration.md
---
fragment-slot: { slot: data-migration, fragment-id: migration, fragment-order: 10 }
requires-capabilities: [data.database.*]
parameters-from: [data.database.name]
---
## Migration Strategy ({{ capabilities.data.database.name }})
... expand/contract, forward-only, naming ...
```

### (c) Composite skill `x-review`
```yaml
# skills/review/x-review/SKILL.md
---
name: x-review
description: Parallel review composed from active specialists
visibility: public
model: sonnet
requires-capabilities: []
fragment-slots: [{ slot: review-specialist, ordering: fragment-order }]
---
# x-review — Orchestrator
Invokes specialists in parallel:
{{ #each fragments.review-specialist }}
- /{{ fragment-id }} — {{ description }}
{{ /each }}

## Specialist details
{{ slot: review-specialist }}
```

### (d) Agent condicional
```yaml
# agents/database-engineer.md
---
name: database-engineer
requires-capabilities: [data.database.*]
parameters-from: [data.database.name]
model: sonnet
---
You are a Database Engineer specializing in {{ capabilities.data.database.name }}...
```

## Forbidden

- Frontmatter sem `requires-capabilities` em diretório condicional. (Universalidade declara `requires-capabilities: []` explicitamente.)
- Capability ID sem `category.subcategory.atomic` formato.
- Glob sem prefixo de categoria (`*.database.postgres` é inválido).
- `fragment-slot` em arquivo fora de `fragments/`.
- `excludes-capabilities` com ID inexistente.
- Schema v2 (sem `requires-capabilities`) — quebra build.

## Permitted

- `requires-capabilities: []` (universal — sempre incluído).
- `requires-any: [a, b]` em vez de mini-DSL `a OR b` (sintaxe primária).
- Múltiplos `fragment-slots` no mesmo artifact-pai.
- `parameters-from` com paths dot-notation profundos (`data.database.parameters.schema-migration-tool`).
- Campos extras desconhecidos (additionalProperties=true) — tolerados para extensibilidade.

## Audit (Layer 4 — CI)

Quatro audit scripts validam o contrato:

| Script | Valida | Exit |
| :--- | :--- | :--- |
| `audit-capability-coverage.sh` | Toda artefato em diretório condicional declara `requires-capabilities` | 0 / 1 / 2 |
| `audit-frontmatter-schema.sh` | 100% dos `.md` válidos contra schema 3.0 | 0 / 1 |
| `audit-capability-graph.sh` | Refs apontam para capabilities reais; mutex simétrico; sem ciclos | 0 / 1 / 2 / 3 |
| `audit-fragment-coherence.sh` | Slots declarados ↔ refs existem ↔ fragments em disco | 0 / 1 |

Catálogo em `docs/audit-gates-catalog.md`.

## Migration

EPIC-0064 Phase 2 (15 stories) migra os 182 artefatos para frontmatter v3.0. Pipeline híbrido AI-assisted + gate humano por categoria (12 PRs). Skill nova `/x-frontmatter-migrate` (story-0064-0202) infere `requires-capabilities` por keyword + heurística de path.

Após Phase 2, audits são hard-fail; antes, advisory.

## Forbidden post-merge

- Adicionar entry em `governance/baselines/capability-coverage-baseline.txt` (vazio + immutable).
- Re-introduzir frontmatter v2 sem `requires-capabilities`.
- Bypassar audit com `--no-verify`.
- Documentar artifact com frontmatter incompatível em CLAUDE.md ou README.

## Self-Audit

`audit-capability-coverage.sh --self-check` verifica:
- Este arquivo (`.claude/rules/28-*.md`) existe.
- Schema `governance/schemas/frontmatter-3.0.json` existe.
- Schema `governance/schemas/capabilities-1.0.json` existe.
- Capabilities catalog `capabilities/_index.yaml` existe.

Falha = `RULE_28_ENFORCEMENT_BROKEN`.
