---
name: ai-memory-production
description: Full AI memory production reference — epic summary contract, _index.yaml, mandatory Phase 5 invocation, enforcement
requires-capabilities: []
---
# AI Memory Production — Full Reference

> **Introduced by:** EPIC-0075
> **ADR:** ADR-0028
> **Capability:** `governance.ai-memory`

## Purpose

Every epic Phase 5 must conclude by producing a compact, indexable memory summary via `x-internal-summarize-epic`. Retrieved by `/x-search-memory`.

## Memory Summary Contract

Every `ai/memory/epic-XXXX-summary.md` MUST:

1. Be produced by `x-internal-summarize-epic` (model: `haiku`) — never written by hand
2. Use `_TEMPLATE-EPIC-MEMORY-SUMMARY.md` as the source template
3. Carry frontmatter v3.0 (Rule 28) with at minimum: `epic-id`, `slug`, `summary-version`, `tags`, `capabilities-affected`, `rules-affected`
4. Have a body of **≤ 200 lines** (skill aborts on overflow)
5. Not contain secrets, tokens, credentials, or PII

## `ai/memory/_index.yaml` Contract

```yaml
schemaVersion: "1.0"
entries:
  - epic-id: EPIC-XXXX
    slug: epic-slug
    summary-path: epic-XXXX-summary.md
    summary-version: "1.0"
    indexable: true
    archived: false
    superseded-by: null
    created: "YYYY-MM-DD"
    last-updated: "YYYY-MM-DD"
```

`indexable: false` excludes the entry from `/x-search-memory` without deleting the file.
`x-internal-summarize-epic` maintains `_index.yaml` atomically on each invocation.

## Mandatory Invocation at Phase 5

Phase 5 of `x-implement-epic` MUST conclude with:

```
Skill(skill: "x-internal-summarize-epic", model: "haiku", args: "<EPIC-ID>")  [required]
```

This is a **MANDATORY TOOL CALL**. Evidence artifact: `ai/memory/epic-XXXX-summary.md`.

Conditional: this invocation is conditional on `governance.ai-memory` capability being active.

## Enforcement

| Camada | Mechanism | Trigger | Exit |
| :--- | :--- | :--- | :--- |
| **0 — PreToolUse** | `enforce-preflight-gates.sh` | `git push`, `gh pr create` | Blocks if memory evidence absent |
| **2 — CI Script** | `audit-memory-coverage.sh` | PR open/sync | `MEMORY_COVERAGE_VIOLATION` |
| **3 — Java Test** | `Epic0075MemoryLayerSmokeIT` | `mvn verify` | JUnit assertion failure |

## Backward Compatibility

`governance.ai-memory` capability defaults to **disabled** for existing projects. Existing projects are completely unaffected.

## Forbidden

- Writing `ai/memory/epic-XXXX-summary.md` by hand instead of via `x-internal-summarize-epic`
- Summaries exceeding 200 lines
- Storing secrets, tokens, or PII in any summary file
- Manually editing `ai/memory/_index.yaml` entries without running `x-internal-summarize-epic`
