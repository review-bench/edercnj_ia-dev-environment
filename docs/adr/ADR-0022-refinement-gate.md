# ADR-0022 — Refinement Gate Convention

**Status:** Accepted
**Date:** 2026-04-30
**Epic:** EPIC-0069 (Story Refinement & DoR Gate)
**Rule:** Rule 29 — Refinement Gate
**Replaces:** (none — new decision)

---

## Context

Stories enter `x-story-implement` without proof of refinement. The planning artifacts (arch/impl/tests/tasks/security/compliance) validate implementation coherence but not story quality: vague acceptance criteria, untyped contracts, missing metrics, and absent alternatives are undetectable by current gates.

The result (observed across multiple sprints):
- AC with no boundary/error scenarios → TDD encodes assumptions, not requirements
- Generic contracts (`Map<String, Any>`) → dev-time refinement delay
- No metrics → unmeasurable success
- No alternatives → architectural debt discovered during implementation

A checklist (`_TEMPLATE-DOR-CHECKLIST.md`) existed but had no enforcement: it described without blocking.

## Decision

Introduce a **blocking refinement gate** (`governance.refinement-gate`) with:

1. **Multi-persona dispatcher skills** `/x-story-refine` and `/x-epic-refine` — single-entry orchestrators that internally dispatch specialist persona-agents (PO, Tech Lead, Architect, Security, QA + conditional Performance/SRE) in parallel, consolidate gap-reports into a single batch for the operator, collect answers once, and have the Architect agent produce the final `## Refinement Verdict` block.

2. **`refinementVerdict` field** in `execution-state.json` — dual-write (markdown + state) for hook-accessible audit trail. `verdictHash` detects manual divergence between the two.

3. **PreToolUse hook** `enforce-refinement-gate.sh` (Camada 0, exit `33`) — blocks `x-story-implement`, `x-epic-implement`, `x-task-implement`, `x-epic-orchestrate` when `refinementVerdict.status ≠ "approved"`.

4. **CI audit** `audit-refinement-gate.sh` (Camada 2, exit `REFINEMENT_GATE_VIOLATION`) — verifies `verdictHash` integrity for merged PRs.

5. **Status `Refinada`** added to the lifecycle state machine (Rule 29 §State Machine Extension), positioned between `Pendente` and `Planejada`.

## Alternatives Considered

### A. Static checklist validator (no interaction)
Reads story markdown and generates warnings for missing dimensions. **Rejected:** produces warnings, not dialogue; operators skip warnings habitually. Does not force deliberate resolution.

### B. LLM-automated refinement without operator (full automation)
Skill auto-fills all dimensions by inferring from story context. **Rejected:** destroys the value of refinement — the point is deliberate operator input, not LLM inference. The story would *look* refined while encoding LLM assumptions.

### C. Single-persona sequential questionnaire
One voice (e.g., PO) asks all questions in a loop. **Rejected:** misses domain-specific NO-GOs (Security knows PII rules; QA knows AC completeness). Loop interaction produces "wizard fatigue" — operators click through quickly.

### D. N public skills (`x-story-refine-po`, `x-story-refine-security`, …)
Each persona is a separate invocable skill. **Rejected:** operator must invoke 5+ commands per story; not a gate, a suggestion. Taxonomy explosion (D-R4 preserves 10 canonical categories).

### E. Validation inline in each orchestrator (Phase 0 check)
Each of `x-story-implement`, `x-epic-implement`, etc. reads `execution-state.json` and fails early. **Rejected:** 4 copies of the same logic → guaranteed drift within 2 sprints. Hook centralizes; orchestrators remain consumers.

## Decision Rationale

The chosen approach (multi-persona dispatcher + PreToolUse hook) is the minimal system that:
- Forces deliberate operator participation (not automated)
- Applies domain-specific NO-GOs without operator confusion
- Presents questions once (single batch, not iterative loop)
- Centralizes enforcement (hook, not per-orchestrator duplication)
- Persists proof of refinement (state + markdown dual-write)
- Is backward-compatible (`flowVersion=1` → hook no-op; `hotfix/*` → bypass documented)

## Consequences

### Positive
- Stories arrive at `x-story-implement` with measurable AC, typed contracts, and identified risks
- Retrabalho silencioso in Phase 2 (TDD loop) decreases — proxy metric: ≥30% reduction in Phase 2 time over 5 epics
- `execution-state.json` carries `refinementVerdict` — hooks and CI audit can read state without parsing markdown
- Multi-persona approach catches Security NO-GOs (PII, credential handling) that single-voice misses

### Negative / Trade-offs
- **Interactive requirement:** refinement cannot be fully automated in CI pipelines. Projects with fully automated flows must pre-refine manually or use `--legacy-refinement` (`flowVersion=1`)
- **Latency:** adding a refinement phase before planning extends time-to-implementation for new stories; offset by reduced rework
- **Persona files must have §Rules:** `agents/core/*.md` must declare NO-GOs explicitly; if absent, NO-GO logic is inferred from loaded rules (acceptable degradation)

### Neutral
- Dogfood: the 7 stories of EPIC-0069 are the first to be refined by the skill they deliver (post-merge)
- `_TEMPLATE-DOR-CHECKLIST.md` is superseded by the `## Refinement Verdict` block; formal retirement deferred to EPIC-0070

## Implementation Notes

- Capability: `governance.refinement-gate` (universal, `requires-capabilities: []`)
- Rule: Rule 29 (`src/main/resources/targets/claude/rules/29-refinement-gate.md`)
- ADR: this file (`docs/adr/ADR-0022-refinement-gate.md`)
- Stories implementing: EPIC-0069 (0001–0007)

## Update History

| Date | Change |
| :--- | :--- |
| 2026-04-30 | Initial acceptance (EPIC-0069 story-0069-0001) |
