---
name: kp-lifecycle-spec-driven-planning
description: "Normative contract for spec-driven planning disciplines (Planning Depth Tier + Knowledge Verification Chain) layered on top of, and strictly subordinate to, Rule 27 zero-bypass and Rule 29 refinement gate."
requires-capabilities: []
---

# Knowledge Pack: Spec-Driven Planning (Lifecycle Contract)

> **Subordination clause.** This contract is **strictly subordinate** to
> [`zero-bypass.md`](zero-bypass.md) (Rule 27) and [`refinement-gate.md`](refinement-gate.md)
> (Rule 29). It adds disciplines; it never removes a mandatory artifact, orchestration
> surface, evidence path, gate, or telemetry marker. On any conflict, zero-bypass and
> the refinement gate prevail.

## Why this exists

Adapted from the open-source `tlc-spec-driven` skill (CC-BY-4.0, Felipe Rodrigues,
`tech-leads-club/agent-skills`). We adopt its discipline (adapt depth to complexity,
resolve ambiguity early, never fabricate, trace requirements) but **reject** its
adaptive auto-sizing / Quick Mode because phase-skipping violates Rule 27. Full
rationale: [`ADR-0050`](../../../docs/adr/ADR-0050-spec-driven-adoption.md).
Full operational detail: `@spec-driven-kp`.

## Two mandatory disciplines

### D1 — Planning Depth Tier (mandatory at planning start)

Every epic and story is classified **TIER-1 Trivial | TIER-2 Standard | TIER-3
Complex** before plan artifacts are produced. The tier tunes **content depth only**.
All artifacts, surfaces, gates, and telemetry of the full lifecycle still apply to
TIER-1. Tier + driving trigger are recorded in the artifact header
(`**Planning Depth Tier:**`) and `execution-state.json` (`planningDepthTier`).
Default when uncertain: **TIER-2**. Any single TIER-3 trigger ⇒ TIER-3.

### D2 — Knowledge Verification Chain (mandatory before technical assertions)

Before a planning/architecture/task-plan artifact asserts a technical fact, the fact
is sourced by walking, in order: codebase → project docs → MCP/Context7 → web →
**flag `UNVERIFIED` (never fabricate)**. TIER-2/TIER-3 artifacts carry a
`## Knowledge Verification Provenance` table; TIER-1 may collapse it to one
`verified against:` line.

## Gray-Area Discovery (advisory, feeds existing gates)

User-facing ambiguity is detected and resolved (one batched `AskUserQuestion`,
Rule 20 / ADR-0010) **before design**, captured in story `### 8.1 Gray Area Decisions`.
This feeds the **existing** `ac`/`value` heuristics in
[`../refinement/dimensions.md`](../refinement/dimensions.md). It does **not** add a key
to `refinementVerdict.dimensions`; the Rule 29 verdict schema and `verdictHash` are
unchanged.

## Requirement Traceability (advisory)

`REQ-<DOMAIN>-NN` IDs flow spec → AC → AT/UT → commit trailer `Requirement:`.
Additive to `RULE-NNN` and `Task:` trailers; changes no existing audit.

## Enforcement posture

| Discipline | Posture | Mechanism |
| :--- | :--- | :--- |
| D1 Tier classification | Required field; advisory audit | Header field + `planningDepthTier` in state; plan skills assert presence |
| D2 Verification chain | Required for TIER-2/3 technical artifacts | Provenance block presence checked by plan skills / review |
| Gray-Area capture | Advisory; influences refinement verdict via existing dimensions | `x-refine-*` persona heuristics |
| REQ traceability | Advisory | Traceability table; commit trailer |

No new CI exit code is introduced. No existing audit (`EIE_*`, `RA9_*`,
`REFINEMENT_GATE_*`, lifecycle-integrity) changes behaviour. Absence of a tier field or
provenance block is a **planning-quality** finding raised by plan/review skills, not a
zero-bypass violation.
