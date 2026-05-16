---
name: x-refine-epic
description: "Multi-persona 4-phase strategic epic refinement: parallel specialists + verdict."
visibility: public
user-invocable: true
model: sonnet
allowed-tools: [Read, Write, Edit, Skill, Agent, TaskCreate, TaskUpdate, Bash]
argument-hint: "<EPIC-ID> [--non-interactive] [--dry-run] [--legacy-refinement]"
requires-capabilities: [governance.refinement-gate]
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, Concise.
- **Efficiency**: Remove all conversational fillers. Start directly with technical content.

# Skill: Refine Epic (slim — ADR-0012)

## Triggers

```text
/x-refine-epic epic-0069                        — full 4-phase strategic refinement (interactive)
/x-refine-epic epic-0069 --non-interactive      — phases A+D only (no operator questions)
/x-refine-epic epic-0069 --dry-run              — run A-D without writing markdown or state
/x-refine-epic epic-0069 --legacy-refinement    — skip all phases; return verdict.status="tbd"
```

## Parameters

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `EPIC-ID` | positional | (required) | `epic-XXXX` or bare `0069` format |
| `--epic-id` | String | auto-derived | 4-digit epic ID override |
| `--non-interactive` | boolean | `false` | Skip Phase B+C (no operator questions); verdict based on Phases A+D only |
| `--dry-run` | boolean | `false` | Execute Phases A–D without writing markdown or state |
| `--legacy-refinement` | boolean | `false` | Skip all phases; emit WARNING; return `verdict.status="tbd"` |

## Output Contract

| Field | Type | Description |
|-------|------|-------------|
| `epicId` | String | Resolved epic ID |
| `verdict` | Enum | `approved \| rejected \| tbd` |
| `verdictHash` | String | SHA-256 of verdict JSON (for CI drift detection) |
| `blockers` | List\<String\> | NO-GO dimensions (empty if approved) |
| `refinedAt` | ISO-8601 | Timestamp of final verdict |
| `artifactPath` | String | Path to epic markdown `## Refinement Verdict` section |

## Error Codes

| Exit | Code | Condition |
|------|------|-----------|
| 1 | `EPIC_NOT_FOUND` | Epic markdown not found at expected path |
| 2 | `EPIC_STATE_MISSING` | `execution-state.json` not found for epic |
| 3 | `PHASE_A_EMPTY` | All personas returned empty gap-reports |
| 4 | `VERDICT_WRITE_FAILED` | x-internal-update-status returned non-zero |

## CRITICAL EXECUTION RULE

**4 phases (A–D). ALL phases through Phase D are mandatory unless `--non-interactive` skips Phase B+C.** Print `>>> Phase X completed. Proceeding to Phase Y...` between phases. `>>> Phase D completed. Refinement verdict written.` at end.

`--legacy-refinement`: emit WARNING and return immediately with `verdict.status="tbd"` — no phases execute.

## Workflow Overview

```text
Phase A: STRATEGIC ANALYSIS   -> Resolve epic path/state; launch 5-6 parallel persona agents
                                 (PO, TechLead, Architect, Security, QA + conditional SRE).
                                 Each returns JSON gap-report (questions vs silent NO-GOs).
Phase B: CONSOLIDATE + Q&A    -> Dedup, group by category (Problema/Hipótese/...); ONE
                                 AskUserQuestion batch. SKIPPED when --non-interactive.
Phase C: STRATEGIC REFINEMENT -> Re-launch persona agents with operator answers; collect
                                 proposedSections per persona. SKIPPED when --non-interactive.
Phase D: ARCHITECT CONSOLIDATE -> Opus-tier architect agent merges sections, evaluates NO-GOs,
                                  produces final Refinement Verdict JSON.
                                  Dual-write: execution-state.json + epic markdown (skipped on --dry-run).
```

> **Gray-Area input (`@spec-driven-kp` §2).** Phase A persona agents MUST treat
> unresolved user-facing ambiguity as a NO-GO under the **existing** strategic
> dimensions (value/problem/alternatives). This adds **no** key to
> `refinementVerdict.dimensions` — the schema and `verdictHash` are unchanged
> (Rule 29 contract preserved).

## Phases A–D

The detailed inline protocol for each phase (full persona Agent() prompts with all dimension-specific NO-GO rules, telemetry sub-markers per persona, Phase B dedup heuristic, Phase D consolidator algorithm with section merge targets and the verdict format) lives in [`references/full-protocol.md`](references/full-protocol.md):

- **Phase A — Parallel Strategic Analysis** (§A): probe `epic-XXXX*/epic-XXXX.md` glob (→ `EPIC_NOT_FOUND` on miss); probe `execution-state.json` (→ `EPIC_STATE_MISSING`); read dimensions KP; emit **ALL 5-6 persona `Agent()` calls as SIBLING tool calls in ONE assistant message** (PO+TechLead+Architect+Security+QA always; SRE conditional on `infra.observability.*` OR `infra.deploy.*`); split returns into `questions` vs `noGos` (NO-GOs silent — never surface as questions, per D-R15); pre/post phase gates via `x-internal-verify-phase-gates`.
- **Phase B — Consolidate & Operator Q&A** (§B): SKIP when `--non-interactive` (set `answers = {}`). Otherwise: dedup by semantic similarity, group by 6 strategic categories (Problema, Hipótese/OKRs, Alternativas, Segurança, Qualidade, Operações), discard empties. **EXACTLY ONE** `AskUserQuestion` batch (D-R14, no per-persona loops). Parse reply into `{Qn: answer}` map.
- **Phase C — Parallel Strategic Refinement** (§C): SKIP when `--non-interactive` (set `proposedSections = {}`). Otherwise: re-launch persona agents in parallel (SIBLING calls) with each persona's own answers; collect `proposedSections` per dimension; merge into single map. Pre/post phase gates.
- **Phase D — Architect Consolidation + Dual-Write** (§D): **opus-tier** architect agent merges sections, evaluates NO-GOs vs operator answers, produces final Refinement Verdict JSON conforming to 7-dimension schema (problem/persona/value/okrs/alternatives/risks/scope). Compute `verdictHash` (`sha256sum`). **Dual-write (skipped on `--dry-run`)**: (1) `x-internal-update-status` INLINE-SKILL writes verdict to `execution-state.json` → `VERDICT_WRITE_FAILED` on non-zero; (2) Edit tool replaces/appends `## Refinement Verdict` block in epic markdown with `verdictHash` populated. Final phase gate with `--expected-artifacts`.

## Output Summary

Print a concise summary:

```text
x-refine-epic completed for epic-XXXX
  Verdict: <approved|rejected|tbd>
  Blockers: <none | list>
  Verdict hash: <hash>
  State file: ai/epics/epic-XXXX.../execution-state.json (field: refinementVerdict.scope=epic)
  Epic markdown: {epicPath} (section: ## Refinement Verdict)
```

## Knowledge Pack References

Read `.claude/knowledge/refinement/dimensions.md` (§Epic Dimensions) — persona-to-dimension mapping and NO-GO rules. Read `.claude/knowledge/lifecycle/refinement-gate.md` for the gate contract and CI drift detection.

## Integration Notes

- **Consumed by:** `x-implement-epic` (Phase 0: checks `refinementVerdict.status == "approved"` AND `scope == "epic"` via `enforce-refinement-gate.sh` hook).
- **Parallel with:** `x-refine-story` (distinct write paths; shared `dimensions.md` KP is read-only).
- **Depends on:** `x-internal-update-status` (INLINE-SKILL, Rule 13 Pattern 1), `knowledge/refinement/dimensions.md` (KP, story-0069-0001).
- **Verdict drift detection:** `verdictHash` in state file vs `verdictHash` in epic markdown is compared by `audit-refinement-gate.sh` (story-0069-0006). Drift fails CI with `REFINEMENT_VERDICT_DRIFT`.
- **Re-run idempotency:** Running `x-refine-epic` again on an `approved` epic replaces the verdict. This is intentional — re-refinement after epic scope changes should update the verdict.
- **Scope discriminator:** This skill always writes `scope: "epic"`. The gate `enforce-refinement-gate.sh` checks both `status == "approved"` AND `scope == "epic"` to unblock `x-implement-epic`. Story-level `scope: "story"` verdicts do NOT unblock epic-level gates.

## Full Protocol

Minimum viable contract above. Detailed phase-by-phase workflow (full 6 persona `Agent()` prompts with dimension-specific NO-GO rules, telemetry sub-markers per persona, Phase B dedup heuristic + question grouping table, Phase D architect consolidator prompt with section merge targets, verdict JSON schema + Refinement Verdict markdown format) lives in [`references/full-protocol.md`](references/full-protocol.md) per ADR-0012 (skill body slim-by-default).
