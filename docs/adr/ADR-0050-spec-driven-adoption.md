# ADR-0050 — Selective Adoption of `tlc-spec-driven` Concepts

## Status

Accepted

## Context

The open-source agent skill **`tlc-spec-driven`** (author Felipe Rodrigues, Tech Leads
Club — `tech-leads-club/agent-skills`, licensed **CC-BY-4.0**) implements spec-driven
development with an *adaptive, auto-sizing* 4-phase pipeline (SPECIFY → DESIGN → TASKS →
EXECUTE) that **skips phases** based on a Small/Medium/Large/Complex assessment, plus a
"Quick Mode" express lane for ≤3-file changes.

It carries five concepts of clear value to our planning, ideation, refinement, and
story-authoring stages:

1. Adaptive depth by complexity.
2. Gray-area (user-facing ambiguity) discovery *before* design.
3. A Knowledge Verification Chain that forbids fabricating technical facts.
4. End-to-end requirement traceability IDs.
5. A lightweight deferred-ideas / scope-creep ledger.

However, its central mechanism — phase auto-skip and Quick Mode — conflicts **head-on**
with [`knowledge/lifecycle/zero-bypass.md`](../../src/main/resources/claude/knowledge/lifecycle/zero-bypass.md)
(Rule 27), which is declared *INEGOCIÁVEL*: every work item must traverse the full
orchestrated lifecycle with its mandatory artifacts, evidence paths, and telemetry.
EPIC-0078 (slim-rules) and the open refinement-contract work are concurrently touching
the lifecycle/refinement rules, raising collision risk for any change to the rule files
or the `refinementVerdict` schema.

## Decision

**Adopt the five disciplines as content/quality conventions; reject the auto-sizing /
Quick Mode mechanism.**

- Introduce **Planning Depth Tier** (TIER-1/2/3) that scales *content depth only* —
  never artifact existence. Every artifact, surface, gate, and telemetry marker of the
  zero-bypass lifecycle still applies to TIER-1.
- Adopt **Gray-Area Discovery** as a pre-design protocol whose output feeds the
  **existing** `ac`/`value` refinement heuristics. It does **not** add a key to
  `refinementVerdict.dimensions`; the Rule 29 verdict schema and `verdictHash` are
  unchanged.
- Adopt the **Knowledge Verification Chain** (codebase → docs → MCP → web → flag
  `UNVERIFIED`) with a provenance block in plan/architecture/task-plan artifacts.
- Adopt **`REQ-<DOMAIN>-NN` traceability**, additive to `RULE-NNN` and the `Task:`
  commit trailer.
- Adopt the **Deferred-Ideas ledger** backed by `execution-state.json.deferredIdeas`.

Vehicle: a new internal knowledge pack `@spec-driven-kp` plus the normative lifecycle
contract `knowledge/lifecycle/spec-driven-planning.md`, **subordinate** to Rule 27 and
Rule 29. No new numbered Rule file is created (the lifecycle "rules" are knowledge docs,
and EPIC-0078 is actively renumbering/slimming them — a new rule file would collide).
No `zero-bypass.md` / `refinement-gate.md` edits (collision avoidance); discoverability
is via outward cross-links and skill `@spec-driven-kp` references.

## Alternatives Considered

- **Adopt auto-sizing + Quick Mode verbatim.** Rejected: directly violates Rule 27
  (zero-bypass, *INEGOCIÁVEL*). Phase-skipping defeats CI evidence audits
  (`EIE_EVIDENCE_MISSING`) and telemetry continuity.
- **Add a new numbered Rule `NN-spec-driven-planning.md`.** Rejected: lifecycle rules
  are knowledge docs, not standalone files; EPIC-0078 is renumbering them — high
  collision risk and structural mismatch.
- **Add a `gray-area` key to `refinementVerdict.dimensions`.** Rejected: changes the
  Rule 29 contract and `verdictHash`, risking `Epic0069RefinementGateSmokeIT` and the
  in-flight refinement-contract PR.
- **Advisory document only, no integration.** Rejected: would not change behaviour in
  the planning skills the user wants strengthened.

## Consequences

- **Positive:** depth scales with risk; ambiguity resolved once before design; technical
  fabrication is explicitly forbidden; requirements are traceable spec→test→commit;
  scope creep is captured, not silently dropped. Zero-bypass and the refinement gate
  are provably preserved (no schema/audit/exit-code change).
- **Negative / trade-offs:** TIER-1 work still produces the full artifact set (we
  intentionally do not get the original skill's brevity at the lifecycle level — only at
  the content level). New header field and provenance block add minor authoring
  overhead. Tier/provenance/traceability checks are advisory (plan/review skills),
  not CI-blocking, so enforcement is softer than the zero-bypass core.
- **Attribution:** derivative concepts are credited to `tlc-spec-driven` (CC-BY-4.0) in
  `@spec-driven-kp` and the lifecycle contract.
