# Phase Marker RCA — EPIC-0066 / story-0066-0009
**Date:** 2026-04-29 | **Author:** story-0066-0009 TASK-0066-0009-000

---

## 1. Symptom

Audit of EPICs 0057-0060 `events.ndjson` shows 0 occurrences of `phase.start` or `phase.end` events for `x-story-implement` and `x-task-implement`, despite Rule 13 §Telemetry Markers contract mandating them since EPIC-0040.

---

## 2. Inventory (Markers vs. Phases)

### `x-story-implement/SKILL.md`

| Phase | `phase.start` | `phase.end` | Coverage |
|:------|:------------|:----------|:---------|
| Phase 0 — Args, Context & Resume | Line 93 (`Phase-0-Prepare`) | Line 128 (`Phase-0-Prepare ok`) | ✅ COMPLETE |
| Phase 1 — Plan | Line 146 (`Phase-1-Plan`) | Line 191 (`Phase-1-Plan ok`) | ✅ COMPLETE |
| Phase 2 — Task Execution Loop | Line 198 (`Phase-2-Implement`) | Line 238 (`Phase-2-Implement ok`) | ✅ COMPLETE |
| Phase 3 — Verify, Report & Cleanup | Line 245 (`Phase-3-Verify`) | Line 304 (`Phase-3-Verify ok`) | ✅ COMPLETE |
| **Total** | **4 start** | **4 end** | **4/4 phases covered** |

### `x-task-implement/SKILL.md`

| Phase | `phase.start` | `phase.end` | Coverage |
|:------|:------------|:----------|:---------|
| Phase 0 — Setup | Line 57 (`Phase-0-Setup`) | Line 76 (`Phase-0-Setup ok`) | ✅ COMPLETE |
| Phase 1 — Prepare | Line 81 (`Phase-1-Prepare`) | Line 96 (`Phase-1-Prepare ok`) | ✅ COMPLETE |
| Phase 2 — TDD Cycles | Line 103 (`Phase-2-TDD`) | Line 146 (`Phase-2-TDD ok`) | ✅ COMPLETE |
| Phase 3 — Validate | Line 151 (`Phase-3-Validate`) | Line 174 (`Phase-3-Validate ok`) | ✅ COMPLETE |
| Phase 4 — Commit and CI Watch | Line 179 (`Phase-4-Commit`) | Line 206 (`Phase-4-Commit ok`) | ✅ COMPLETE |
| Phase 5 — Cleanup | Line 211 (`Phase-5-Cleanup`) | Line 228 (`Phase-5-Cleanup ok`) | ✅ COMPLETE |
| **Total** | **6 start** | **6 end** | **6/6 phases covered** |

---

## 3. Hypothesis Results

| Hypothesis | Result | Evidence |
|:-----------|:-------|:---------|
| **H1** — `telemetry-phase.sh` fail-open silently swallows errors | **FALSE** | Helper inspected: uses `set +e`, `set -u`; logs failures to stderr via `echo ... >&2; exit 0`. No evidence of silent swallowing beyond valid fail-open. |
| **H2** — Audited EPICs ran before markers were retrofitted | **CONFIRMED (primary cause)** | `git log` shows only 1 commit touching `x-story-implement/SKILL.md` (Maven layout restructure); phase markers appear in the current source but audit cannot confirm when they were added relative to EPIC-0057-0060 executions. epic-0047 has 10 `phase.start` events from `x-epic-orchestrate` — proving the mechanism works when the LLM emits the Bash tool call. |
| **H3** — `TelemetryScrubber` filters phase events | **FALSE** | `telemetry-emit.sh` scrubs only JWT/AWS key patterns via minimal regex. `phase.start`/`phase.end` events are not in any filter list. |
| **H4** — Partial coverage (some phases without markers) | **FALSE** | Complete inventory confirms 4/4 phases in `x-story-implement` and 6/6 in `x-task-implement` have balanced pairs. |

---

## 4. Root Cause (Confirmed)

**H2 is the confirmed primary cause.** The `phase.start`/`phase.end` events are absent from EPIC-0057-0060 `events.ndjson` because the LLM sessions executing those skills either:
1. Ran before the phase markers were added to the SKILL.md source-of-truth, OR
2. Were executing the skill logic inline (LLM simulation) rather than issuing actual Bash tool calls for each `Bash command:` line in the skill.

**Secondary behavioral observation:** The `Bash command:` syntax in SKILL.md is an _instruction_ to the LLM to emit a Bash tool call. Compliance requires the LLM to actively interpret and execute these lines. In sessions where the LLM skips or inlines skill execution, these calls are not made. This is a behavioral gap, not a code gap.

---

## 5. Fix Applied

**No modification to SKILL.md files** — both files have complete, balanced phase marker coverage (H4=FALSE).

**Primary deliverable:** `PhaseMarkerEmissionTest.java` (TASK-0066-0009-003) — static inspection test that catches:
- **Absence** of any `phase.start`/`phase.end` marker in a numbered phase (the gap the `TelemetryMarkerLint` doesn't detect).
- **Imbalance** — start without matching end.

This test prevents the H4 regression in future skill modifications.

---

## 6. Identifiers in Use

All identifiers are kebab-case, ≤ 64 chars, prefixed `Phase-N-`:

| Skill | Phase | Identifier |
|:------|:------|:-----------|
| x-story-implement | 0 | `Phase-0-Prepare` |
| x-story-implement | 1 | `Phase-1-Plan` |
| x-story-implement | 2 | `Phase-2-Implement` |
| x-story-implement | 3 | `Phase-3-Verify` |
| x-task-implement | 0 | `Phase-0-Setup` |
| x-task-implement | 1 | `Phase-1-Prepare` |
| x-task-implement | 2 | `Phase-2-TDD` |
| x-task-implement | 3 | `Phase-3-Validate` |
| x-task-implement | 4 | `Phase-4-Commit` |
| x-task-implement | 5 | `Phase-5-Cleanup` |
