---
name: x-internal-summarize-epic
description: "Generates ai/memory/epic-XXXX-summary.md from a completed epic's artifacts."
visibility: internal
user-invocable: false
model: haiku
requires-capabilities: [governance.ai-memory]
allowed-tools: [Read, Write, Bash, Grep, Glob]
---

> 🔒 **INTERNAL SKILL** — Invoked only by other skills via the Skill tool. Not user-invocable.

# x-internal-summarize-epic

Deterministic memory summary generator for completed epics. Parses epic markdown with structured
header extraction (not free-form regex), composes `_TEMPLATE-EPIC-MEMORY-SUMMARY.md`, and writes
`ai/memory/epic-XXXX-summary.md` + updates `ai/memory/_index.yaml`.

**Determinism contract:** Same inputs MUST produce bytewise-identical output across runs.
Use explicit extraction rules; never introduce creative variance.

## Triggers

Invoked by `x-implement-epic` Phase 5 (Rule 33):

    Skill(skill: "x-internal-summarize-epic", args: "--epic-id EPIC-XXXX")

## Parameters

| Flag | Type | Default | Description |
| :--- | :--- | :--- | :--- |
| `--epic-id` | `EPIC-\d{4}` | required | Epic identifier |
| `--out-dir` | path | `ai/memory/` | Output directory override (tests) |
| `--dry-run` | bool | false | Print content without writing files or updating index |
| `--allow-legacy-fallback` | bool | false | For retro-seed (story-0075-0006) only — generates placeholder when v2 Decision sections absent |

## Exit Codes

| Exit | Code | Condition |
| :--- | :--- | :--- |
| 0 | OK | Summary written and index updated |
| 1 | EPIC_NOT_FOUND | `ai/epics/epic-XXXX-*/` absent |
| 2 | SCHEMA_VIOLATION | Generated frontmatter fails `frontmatter-3.0.json` |
| 3 | MEMORY_SUMMARY_TOO_LONG | Body exceeds 200 lines after composition |
| 4 | EXTRACTION_FAILED | Section 6 (Decision Rationale) absent on v2 epic and `--allow-legacy-fallback` not set |
| 5 | INDEX_LOCK_TIMEOUT | Failed to acquire flock on `ai/memory/_index.yaml` within 5s |
| 6 | TEMPLATE_VERSION_MISMATCH | `summary-version` in template changed; mass-regeneration required |
| 7 | MANUAL_REFINEMENT_PRESENT | Summary file contains `<!-- manually-refined: -->` marker; abort to preserve human override |

## Execution Protocol

### Step 1 — Locate epic

```bash
find ai/epics -maxdepth 1 -type d -name "epic-XXXX-*" | head -1
```

Exit 1 `EPIC_NOT_FOUND` when no directory matches.

### Step 2 — Check for manual refinement

Read `ai/memory/epic-XXXX-summary.md` if it exists. If it contains `<!-- manually-refined:`, exit 7
`MANUAL_REFINEMENT_PRESENT`.

### Step 3 — Extract sections (structured parser — deterministic)

Read `ai/epics/epic-XXXX-*/epic-XXXX.md`. Apply extraction rules below in order:

| Target section | Source | Extraction rule |
| :--- | :--- | :--- |
| `## Why this epic existed` | `## 1. Visão & Problema` (v2) or `## Contexto` (v1) | First paragraph only; trim to 5 lines max |
| `## Hypothesis tested` | `## 3. Hipótese & OKRs` (v2) | Extract `If...then...because` line; append `outcome: pending` if no completion report |
| `## Decisions taken (with why)` | `## 8. Decision Rationale` or `## 6.*` sub-sections | Lines starting with `**Decisão:**`, `**Motivo:**`, `**Consequência:**`; if absent and `--allow-legacy-fallback` → placeholder |
| `## Alternatives rejected (with why)` | Same source | Lines starting with `**Alternativa descartada:**` |
| `## Reusable patterns produced` | Same source + keyword scan | Lines matching `/padrão|pattern|convenção/i`; dedup |
| `## Anti-patterns observed` | Same source + keyword scan | Lines matching `/anti-padrão|rejeitado|evitar/i`; dedup |
| `## Links` | Deterministic filesystem + ADR frontmatter | Epic path, `adrs-referenced`, story list, `reports/` dir |

### Step 4 — Assemble frontmatter

Extract deterministically:
- `tags`: normalize headers to taxonomy IDs from `tags-catalog.md`
- `capabilities-affected`: grep `requires-capabilities:` in story files
- `rules-affected`: grep `Rule \d+` in epic markdown
- `adrs-referenced`: grep `ADR-\d{4}` in epic markdown
- `patterns-introduced` / `antipatterns-rejected`: mirror Step 3 extraction
- `dependencies-of` / `dependencies-for`: grep `Depende de:` / `Blocks:`

### Step 5 — Validate and write

1. Count lines in composed summary. If > 200 → exit 3 `MEMORY_SUMMARY_TOO_LONG`.
2. Validate frontmatter against schema. If fails → exit 2 `SCHEMA_VIOLATION`.
3. If `--dry-run`: print to stdout, exit 0.
4. Write `{out-dir}/epic-XXXX-summary.md`.
5. Acquire flock on `{out-dir}/_index.yaml` (5s timeout → exit 5). Update entry:
   - If entry exists: preserve `indexable` and `archived` flags; update `last-updated`.
   - If new entry: add with `indexable: true`, `archived: false`.
6. Release flock. Exit 0.

## Integration Notes

- Invoked by `x-implement-epic` Phase 5 (Rule 33 — MANDATORY TOOL CALL).
- `--allow-legacy-fallback` is for `x-internal-summarize-epic` called from retro-seed (story-0075-0006);
  normal Phase 5 invocations MUST NOT pass this flag.
- Idempotent: re-execution regenerates the summary completely (no append).
- Uses `_TEMPLATE-EPIC-MEMORY-SUMMARY.md` from `.claude/templates/` (present when `governance.ai-memory` active).
