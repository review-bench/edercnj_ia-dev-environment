---
requires-capabilities: []
---
# Rule 33 — AI Memory Production

> **Related:** Rule 13 (Skill Invocation Protocol), Rule 22 (Skill Visibility), Rule 24 (Execution Integrity), Rule 26 (Audit Gate Lifecycle), Rule 27 (Zero-Bypass Lifecycle).
> **Introduced by:** EPIC-0075 (AI Memory Layer).
> **ADR:** [ADR-0028 — AI Memory Layer](../../docs/adr/ADR-0028-ai-memory-layer.md).
> **Capability:** `governance.ai-memory` (`capabilities/governance/ai-memory.yaml`).

## Purpose

Completed epics leave decisions, trade-offs, and patterns scattered across ADRs, stories, PRs, and Slack threads. Re-discovering "why we rejected option X" or "what pattern was introduced by EPIC-0072" costs 15–30 minutes per query — and a new LLM session, lacking historical context, may re-litigate already-decided questions entirely.

Rule 33 mandates that every epic Phase 5 concludes by producing a **compact, indexable memory summary** via `x-internal-summarize-epic`. The summary is optimized for retrieval by both humans and future LLM sessions, covering: problem, hypothesis, decisions with rationale, alternatives rejected, reusable patterns, anti-patterns observed, and semantic links.

The retrieval layer is `/x-search-memory` — a grep-plus-frontmatter index skill that returns relevant summaries by tag, capability, rule, pattern keyword, or epic ID without loading the full `ai/memory/` corpus into context.

## Memory Summary Contract

Every `ai/memory/epic-XXXX-summary.md` MUST:

1. Be produced by `x-internal-summarize-epic` (model: `haiku`, deterministic) — never written by hand.
2. Use `_TEMPLATE-EPIC-MEMORY-SUMMARY.md` as the source template.
3. Carry frontmatter v3.0 (Rule 28) with at minimum: `epic-id`, `slug`, `summary-version`, `tags`, `capabilities-affected`, `rules-affected`.
4. Have a body of **≤ 200 lines** (enforced by the skill — abort if exceeded).
5. Not contain secrets, tokens, credentials, or PII (per `knowledge/governance/ai-memory-playbook/index.md`).

## `ai/memory/_index.yaml` Contract

The index is the entry point for `/x-search-memory`. Every `ai/memory/epic-XXXX-summary.md` MUST have a corresponding entry in `ai/memory/_index.yaml`:

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

`indexable: false` excludes the entry from `/x-search-memory` results without deleting the file (manual archiving). `x-internal-summarize-epic` maintains `_index.yaml` atomically on each invocation.

## Mandatory Invocation at Phase 5

Phase 5 of `x-implement-epic` MUST conclude with:

```
Skill(skill: "x-internal-summarize-epic", model: "haiku", args: "<EPIC-ID>")  [required]
```

This is a **MANDATORY TOOL CALL** (Rule 24). Silent omission is a `PROTOCOL_VIOLATION`. Evidence artifact: `ai/memory/epic-XXXX-summary.md` (Rule 27 Surface 14).

Conditional: this invocation is conditional on `governance.ai-memory` capability being active for the project. Projects without this capability are unaffected (Rule 19 safe default).

## Enforcement

| Camada | Mechanism | Trigger | Exit |
| :--- | :--- | :--- | :--- |
| **0 — PreToolUse** | `enforce-preflight-gates.sh` (Rule 24 §Camada 0) | `git push`, `gh pr create`, `Skill x-create-pr` | Blocks if memory evidence absent (when capability active) |
| **1 — Normative** | This rule + CLAUDE.md | Every conversation | — |
| **2 — CI Script** | `audit-memory-coverage.sh` | PR open/sync to `develop` or `epic/*` | 1 `MEMORY_COVERAGE_VIOLATION` |
| **3 — Java Test** | `Epic0075MemoryLayerSmokeIT` | `mvn verify` | JUnit assertion failure |

## Backward Compatibility (Rule 19)

`governance.ai-memory` capability defaults to **disabled** for existing projects. Projects without this capability in their project YAML are unaffected — `audit-memory-coverage.sh` skips them, Phase 5 does not invoke `x-internal-summarize-epic`, and no `ai/memory/` directory is required.

For projects enabling the capability retroactively, the retro-seed workflow (EPIC-0075 story-0075-0006) demonstrates how to generate summaries for already-completed epics.

## Forbidden

- Writing `ai/memory/epic-XXXX-summary.md` by hand instead of via `x-internal-summarize-epic`.
- Summaries exceeding 200 lines (the skill aborts on overflow).
- Storing secrets, tokens, or PII in any summary file.
- Manually editing `ai/memory/_index.yaml` entries without running `x-internal-summarize-epic` to re-validate.
- Omitting the Phase 5 MANDATORY TOOL CALL when `governance.ai-memory` is active.

## Audit

`audit-memory-coverage.sh --self-check` MUST verify:
1. This rule file (`33-ai-memory-production.md`) exists.
2. `capabilities/governance/ai-memory.yaml` exists and is valid.
3. `x-internal-summarize-epic/SKILL.md` exists under skills.

Failure → `RULE_33_ENFORCEMENT_BROKEN`.

---

> **Catalogado em:** [`docs/audit-gates-catalog.md`](../../docs/audit-gates-catalog.md) (entry added by story-0075-0005)