---
requires-capabilities: []
---
# Rule 29 — Refinement Gate

> **Consolidated by:** EPIC-0078 (Context Budget Optimization). **Authoritative contract:** Rule 19 (Lifecycle Integrity Contract).
> **Full detail — state machine, 7 story dimensions, personas, `refinementVerdict` JSON shape, enforcement:**
> `Read src/main/resources/targets/claude/knowledge/lifecycle/refinement-gate.md`
> **Capability:** `governance.refinement-gate` (`capabilities/governance/refinement-gate.yaml`).

## Contract (Non-Negotiable)

Every story/epic MUST have `refinementVerdict.status = "approved"` before `x-implement-story`, `x-implement-epic`, `x-implement-task`, or `x-orchestrate-epic` is invoked. Gate is enforced at Camada 0 (`enforce-refinement-gate.sh`, exit 33 `REFINEMENT_REQUIRED`) and Camada 2 (`audit-refinement-gate.sh`, exit 1 `REFINEMENT_GATE_VIOLATION`).

## Exceptions

`flowVersion=1` legacy epics (Rule 19 fallback) and `hotfix/*` branches — gate is no-op.

## Forbidden

- Manually editing the `## Refinement Verdict` block without re-running `/x-refine-story` or `/x-refine-epic`.
- Introducing bypass env vars for the refinement gate — `CLAUDE_RECOVERY_MODE=1` does not bypass it.