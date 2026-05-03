---
# ── Identidade ──────────────────────────────────────────────────────────────
epic-id: EPIC-{{EPIC_ID}}
slug: {{EPIC_SLUG}}
summary-version: "1.0"
created: "{{CREATED_DATE}}"
last-updated: "{{LAST_UPDATED_DATE}}"

# ── Indexação (mirror de _index.yaml — fonte única no arquivo) ─────────────
indexable: true
archived: false
superseded-by: null

# ── Taxonomia (alimenta /x-memory-search) ───────────────────────────────────
tags: []
capabilities-affected: []
rules-affected: []
adrs-referenced: []

# ── Padrões (indexáveis por busca textual + tags) ───────────────────────────
patterns-introduced: []
antipatterns-rejected: []

# ── Grafo de dependências entre épicos ──────────────────────────────────────
dependencies-of: []
dependencies-for: []
---
# Memory: EPIC-{{EPIC_ID}} — {{EPIC_TITLE}}

<!-- Cap: ≤ 200 lines (frontmatter included). Skill aborts with MEMORY_SUMMARY_TOO_LONG if exceeded. -->

## Why this epic existed

{{WHY_IT_EXISTED}}

## Hypothesis tested

{{HYPOTHESIS_AND_OUTCOME}}

## Decisions taken (with why)

{{DECISIONS_LIST}}

## Alternatives rejected (with why)

{{ALTERNATIVES_LIST}}

## Reusable patterns produced

{{PATTERNS_LIST}}

## Anti-patterns observed

{{ANTIPATTERNS_LIST}}

## Links

- Epic: `ai/epics/epic-{{EPIC_ID}}-{{EPIC_SLUG}}/epic-{{EPIC_ID}}.md`
- ADRs: {{ADR_LINKS}}
- PRs: {{PR_LINKS}}
- Reports: `ai/epics/epic-{{EPIC_ID}}-{{EPIC_SLUG}}/reports/`
