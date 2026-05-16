---
name: kp-governance-capability-composition
description: "Full reference for Rule 28 Capability Frontmatter Contract: capability ID format, glob rules, 4 YAML examples (skill/rule/composite/agent), fragment slot contract, and migration notes."
requires-capabilities: []
---

# Knowledge Pack: Capability Frontmatter Contract (Rule 28 — Full Reference)

## Capability ID Format

Capability IDs MUST follow the `category.subcategory.atomic` format:

| Level | Example | Notes |
| :--- | :--- | :--- |
| `category` | `data` | Top-level domain |
| `subcategory` | `database` | Technology family |
| `atomic` | `postgres` | Specific implementation |
| Full ID | `data.database.postgres` | Always 3 tokens separated by `.` |

Glob patterns: `category.subcategory.*` (all atomics) and `category.subcategory.**` (all sub-levels) are permitted.

## Glob Rules

| Pattern | Valid? | Semantics |
| :--- | :--- | :--- |
| `data.database.*` | yes | All database atomics |
| `data.database.**` | yes | All sub-levels under database |
| `*.database.postgres` | **no** | Glob without category prefix — forbidden |
| `data.*.*` | **no** | Double glob — forbidden |
| `language.java.*` | yes | All Java language capabilities |

## YAML Example (a) — Skill Condicional

```yaml
---
name: x-review-database
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

## YAML Example (b) — Rule Fragmentada

Parent rule (universal core):

```yaml
# rules/core/09-data-management.md
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

Fragment (conditional on capability):

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

## YAML Example (c) — Composite Skill `x-review-codebase`

```yaml
# skills/review/x-review-codebase/SKILL.md
---
name: x-review-codebase
description: Parallel review composed from active specialists
visibility: public
model: sonnet
requires-capabilities: []
fragment-slots: [{ slot: review-specialist, ordering: fragment-order }]
---
# x-review-codebase — Orchestrator
Invokes specialists in parallel:
{{ #each fragments.review-specialist }}
- /{{ fragment-id }} — {{ description }}
{{ /each }}

## Specialist details
{{ slot: review-specialist }}
```

## YAML Example (d) — Agent Condicional

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

## Fragment Slot Contract

- Files under `<artifact-parent>/fragments/` MUST declare `fragment-slot: { slot, fragment-id, fragment-order }`.
- A fragment with `fragment-slot` NEVER generates its own output file — it is injected into the parent body.
- For every `fragment-slots: [{ slot: X }]` in a parent, at least one `{{ slot: X }}` or `{{ #each fragments.X }}` reference must exist in the parent body.
- `audit-fragment-coherence.sh` validates slot ↔ body ↔ fragment-on-disk coherence.

## Scope Table (6 Artifact Types)

| Artefato | Source-of-truth path | Output path |
| :--- | :--- | :--- |
| Skill | `targets/claude/skills/<category>/<name>/SKILL.md` | `.claude/skills/<name>/SKILL.md` |
| Rule | `targets/claude/rules/core/<NN>-<name>.md` | `.claude/rules/<NN>-<name>.md` |
| Knowledge Pack | `targets/claude/knowledge/<name>/index.md` | `.claude/knowledge/<name>/index.md` |
| Agent | `targets/claude/agents/<name>.md` | `.claude/agents/<name>.md` |
| Hook | `targets/claude/hooks/<event>/<name>.sh` | `.claude/hooks/<event>/<name>.sh` |
| Template | `shared/templates/_TEMPLATE-<name>.md` | `.claude/templates/_TEMPLATE-<name>.md` |

## Migration Notes

EPIC-0064 Phase 2 (15 stories) migrated 182 artifacts to frontmatter v3.0 using an AI-assisted pipeline with human gates per category (12 PRs). `/x-migrate-frontmatter` (story-0064-0202) infers `requires-capabilities` by keyword + path heuristics.

After Phase 2, audits are hard-fail. Before, advisory. No dual-mode v2/v3 — schema v3.0 is the only accepted format.
