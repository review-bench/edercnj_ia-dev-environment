---
name: x-memory-search
description: Queries ai/memory/ for decisions, patterns, and anti-patterns across past epics
visibility: public
user-invocable: true
model: haiku
requires-capabilities: [governance.ai-memory]
allowed-tools: [Read, Bash, Grep, Glob]
---

# x-memory-search

Searches `ai/memory/_index.yaml` and the indexed summary files for decisions, patterns,
anti-patterns, or epic context matching the given query.

Results are ranked by relevance (tag match → exact text match → full-text scan) and
filtered by `indexable: true` unless `--include-archived` is passed.

**Determinism contract:** Given the same `ai/memory/` contents and the same flags,
output order is deterministic (alphabetical by epic-id within each relevance tier).

## Triggers

```
/x-memory-search "auth decisions"
/x-memory-search --by-pattern capability-aware-skill-via-frontmatter
/x-memory-search --by-tag governance
/x-memory-search --epic EPIC-0064
/x-memory-search "hexagonal" --include-archived
```

## Parameters

| Flag | Type | Default | Description |
| :--- | :--- | :--- | :--- |
| `QUERY` | string | — | Positional. Free-text search against all summary sections |
| `--by-tag TAG` | string | — | Filter by frontmatter `tags:` value (exact match) |
| `--by-pattern ID` | string | — | Filter by `patterns-introduced` or `antipatterns-rejected` kebab-case ID |
| `--by-epic EPIC-XXXX` | string | — | Return only the summary for that epic |
| `--by-rule RULE-NN` | string | — | Filter by `rules-affected` (e.g. `Rule 28`) |
| `--by-adr ADR-XXXX` | string | — | Filter by `adrs-referenced` |
| `--include-archived` | bool | false | Include entries with `archived: true` or `indexable: false` |
| `--format` | `compact\|full` | `compact` | `compact` = one-line per result; `full` = full summary body |
| `--limit N` | int | 10 | Maximum results to return |

## Exit Codes

| Exit | Code | Condition |
| :--- | :--- | :--- |
| 0 | OK | Results printed (may be empty) |
| 1 | INDEX_NOT_FOUND | `ai/memory/_index.yaml` absent |
| 2 | INVALID_ARGS | Mutually exclusive flags combined, or unknown flag |

## Execution Protocol

### Step 1 — Load index

```bash
cat ai/memory/_index.yaml
```

Exit 1 `INDEX_NOT_FOUND` when absent.

### Step 2 — Filter index entries

Apply filters in order (each narrows the candidate set):

1. Unless `--include-archived`: keep only entries where `indexable: true` AND `archived: false`.
2. `--by-epic`: keep only the matching `epic-id`.
3. `--by-tag`: read each candidate summary; keep if `tags:` list contains the value.
4. `--by-pattern`: keep if `patterns-introduced` or `antipatterns-rejected` contains the ID.
5. `--by-rule`: keep if `rules-affected` contains the value.
6. `--by-adr`: keep if `adrs-referenced` contains the value.
7. Free-text `QUERY`: grep full body of each candidate summary for the term.

### Step 3 — Rank and limit

Sort candidates: exact frontmatter match first, then full-text hits, alphabetically by
epic-id within each tier. Apply `--limit`.

### Step 4 — Format and print

**Compact (default):**
```
EPIC-0064 [governance, capabilities, architecture] — Capability-Driven Composition Refactor
  → patterns: capability-aware-skill-via-frontmatter, requires-capabilities-universal-declaration
```

**Full (`--format full`):**
Print the complete `ai/memory/epic-XXXX-summary.md` body for each result.

## Integration Notes

- Called interactively by operators during epic planning to surface prior decisions.
- Called by `x-arch-plan` to auto-inject relevant memory context into architecture plans
  (conditional: `governance.ai-memory` active).
- Output is read-only; does not modify `_index.yaml` or any summary file.
- Empty result set is a valid response (exit 0, no output).

## Examples

```
# Find all governance decisions
/x-memory-search --by-tag governance

# Find who rejected inline-validation
/x-memory-search --by-pattern inline-validation-instead-of-hook

# Find all epics that reference Rule 28
/x-memory-search --by-rule "Rule 28"

# Full body for one epic
/x-memory-search --by-epic EPIC-0064 --format full
```
