# ADR-0033 — Rules as Short Contracts

**Status:** Accepted  
**Date:** 2026-05-06  
**Epic:** EPIC-0078 (Context Budget Optimization) — story-0078-0016  
**Supersedes:** Informal convention (no prior ADR)

## Context

Claude Code rules (`.claude/rules/*.md`) are loaded into every conversation as part of the always-loaded context budget. Before EPIC-0078, rules freely expanded to include:

- Multi-paragraph decision rationale
- Historical narrative ("Before EPIC-X, this happened")
- Full audit script source listings
- Repeated cross-references already covered by sibling rules
- Example code blocks exceeding what the LLM needs to act correctly

A measurement taken at the start of EPIC-0078 showed **59,591 tokens always-loaded** — more than double the 25,000-token target. Each token costs latency and money on every invocation.

The root cause is a conflation of two distinct concerns:

| Concern | Correct home |
| :--- | :--- |
| The **contract** (what MUST be done) | Rule file — always loaded |
| The **rationale** (why it exists, history) | KP or ADR — lazy-loaded on demand |

Rules grew into mini-ADRs because there was no normative guidance stating they MUST stay short.

## Decision

Rules MUST be **short normative contracts**, not narrative documents. The following limits and structural invariants are binding from EPIC-0078 story-0078-0016 onward:

### Size Limits

| Artifact | Limit | Enforcement |
| :--- | :--- | :--- |
| Any single rule file | ≤ 120 lines | `audit-context-budget.sh` |
| Total always-loaded budget | ≤ 25,000 tokens | `audit-context-budget.sh` (hard-fail, default since story-0078-0016) |

### Mandatory Structure (4 Blocks)

Every rule file MUST contain exactly four blocks, in this order:

1. **Purpose** — one paragraph, ≤ 5 sentences. States WHAT the rule enforces and WHY it matters in production. No historical narrative.
2. **Invariants / Forbidden** — bullet list of normative constraints. Each item starts with a verb ("MUST", "MUST NOT", "NEVER"). No inline rationale — if rationale is complex, link to the ADR.
3. **Enforcement** — table: Camada / Mechanism / Trigger / Exit code. Exactly one row per enforcement layer.
4. **Reference** — single-line cross-links: related rules, ADR number, KP path. No prose.

### Content Constraints

- **No inline history.** "Before EPIC-X" paragraphs MUST move to the corresponding ADR or be deleted.
- **No duplicated cross-references.** A rule that references Rule A MUST NOT repeat Rule A's content.
- **No audit script source.** Script behavior is described in one Enforcement row. Full script lives at its canonical path.
- **No fallback matrices.** Fallback logic for `execution-state.json` fields belongs in the ADR for the feature that introduced the field (Rule 19 is the exception — it IS the fallback registry).

### `_TEMPLATE-RULE.md`

`src/main/resources/shared/templates/_TEMPLATE-RULE.md` is the authoritative template for new rules. Authors MUST start from it. The CI audit `audit-template-version.sh` is extended to flag rule files that deviate from the 4-block structure (warn-only in first release, hard-fail in second).

## Alternatives Considered

### Alternative 1 — Lazy-load all rules (never always-loaded)

Rejected. Rules define the behavioral contract the LLM must follow in EVERY turn. A rule that is not loaded cannot constrain behavior. Lazy-loading introduces a race: a rule might not load before the first tool call it is supposed to govern.

### Alternative 2 — Compress rules losslessly (minify markdown)

Rejected. Minification degrades readability for human authors and reduces the LLM's ability to parse intent from context. Structural clarity is a correctness property, not a style preference.

### Alternative 3 — Increase the budget limit

Rejected. A higher limit normalizes bloat. The purpose of the budget is to create enforcement pressure that causes authors to move rationale to KPs/ADRs where it belongs.

## Consequences

### Positive

- Always-loaded budget drops from 59,591 → target ≤ 25,000 tokens.
- Rules become scannable: operators can read a rule in under 30 seconds.
- ADRs receive the historical narrative that belongs to them.
- KPs receive the reference content that the LLM should look up, not memorize.

### Negative

- Existing rules must be migrated. EPIC-0078 stories 0001–0013 perform the migration.
- The 4-block structure is opinionated; some rules with complex fallback matrices (Rule 19, Rule 29) exceed it structurally. They are grandfathered via the rule-size baseline until a dedicated epic migrates them.

## Compliance

`audit-context-budget.sh` enforces this ADR:

- Default mode is hard-fail (story-0078-0016).
- `governance/baselines/context-budget.json` sets `limit: 25000`.
- New rules that push always-loaded above 27,500 tokens (25,000 + 10% tolerance) fail CI.

Baseline `governance/baselines/rule-size-baseline.txt` (to be introduced in a follow-up epic) will grandfather existing over-limit rules.

---

> **Related:** Rule 26 (Audit Gate Lifecycle) · EPIC-0078 · `governance/baselines/context-budget.json`
