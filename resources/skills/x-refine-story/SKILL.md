---
name: x-refine-story
description: "Multi-persona 4-phase story refinement: parallel specialists + verdict."
visibility: public
user-invocable: true
model: sonnet
allowed-tools: [Read, Write, Edit, Skill, Agent, TaskCreate, TaskUpdate, Bash]
argument-hint: "<STORY-ID> [--epic-id <ID>] [--non-interactive]"
requires-capabilities: [governance.refinement-gate]
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, Concise.
- **Efficiency**: Remove all conversational fillers. Start directly with technical content.

# Skill: Refine Story (slim — ADR-0012)

## Triggers

```text
/x-refine-story story-XXXX-YYYY                        — full 4-phase refinement (interactive)
/x-refine-story story-XXXX-YYYY --non-interactive       — phases A+D only (no operator questions)
/x-refine-story story-XXXX-YYYY --epic-id 0069          — explicit epic ID override
```

## Parameters

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `STORY-ID` | positional | (required) | `story-XXXX-YYYY` format |
| `--epic-id` | String | auto-derived from STORY-ID prefix | 4-digit epic ID |
| `--non-interactive` | boolean | `false` | Skip Phase B+C (no questions); verdict based on Phases A+D only |

## Output Contract

| Field | Type | Description |
|-------|------|-------------|
| `storyId` | String | Input story ID |
| `epicId` | String | Resolved epic ID |
| `verdict` | Enum | `approved \| rejected \| tbd` |
| `verdictHash` | String | SHA-256 of verdict JSON (for CI drift detection) |
| `blockers` | List\<String\> | NO-GO dimensions (empty if approved) |
| `refinedAt` | ISO-8601 | Timestamp of final verdict |
| `artifactPath` | String | Path to story markdown `## Refinement Verdict` section |

## Error Codes

| Exit | Code | Condition |
|------|------|-----------|
| 1 | `STORY_NOT_FOUND` | Story markdown not found at expected path |
| 2 | `EPIC_STATE_MISSING` | `execution-state.json` not found for epic |
| 3 | `PHASE_A_EMPTY` | All personas returned empty gap-reports |
| 4 | `VERDICT_WRITE_FAILED` | x-internal-update-status returned non-zero |

## CRITICAL EXECUTION RULE

**4 phases (A–D). ALL phases through Phase D are mandatory unless `--non-interactive` skips Phase B+C.** Print `>>> Phase X completed. Proceeding to Phase Y...` between phases. `>>> Phase D completed. Refinement verdict written.` at end.

## Workflow Overview

```text
Phase A: SPECIALIST ANALYSIS  -> Resolve story path; launch 5-7 parallel persona agents
                                 (PO, TechLead, Architect, Security, QA + conditional
                                 Performance, DevOps). Each returns JSON gap-report
                                 (questions vs silent NO-GOs).
Phase B: CONSOLIDATE + Q&A    -> Dedup, group by 6 dimension categories; ONE
                                 AskUserQuestion batch. SKIPPED when --non-interactive.
Phase C: SPECIALIST REFINEMENT-> Re-launch persona agents with operator answers;
                                 collect proposedSections. SKIPPED when --non-interactive.
Phase D: ARCHITECT CONSOLIDATE -> Opus-tier architect agent merges sections, evaluates
                                  NO-GOs, produces final Refinement Verdict JSON.
                                  Dual-write: execution-state.json + story markdown.
```

## Phases A–D

The detailed inline protocol for each phase (full persona Agent() prompts with dimension-specific NO-GO rules, telemetry sub-markers per persona, Phase B question grouping, Phase D consolidator algorithm with section merge targets and the verdict format) lives in [`references/full-protocol.md`](references/full-protocol.md):

- **Phase A — Parallel Specialist Analysis** (§A): probe `epic-XXXX*/story-XXXX-YYYY.md` glob (→ `STORY_NOT_FOUND` on miss); read dimensions KP; emit **ALL 5–7 persona `Agent()` calls as SIBLING tool calls in ONE assistant message** (PO+TechLead+Architect+Security+QA always; Performance conditional on `flag.has_sla_declared`; DevOps conditional on `flag.has_infra_changes`); split returns into `questions` vs `noGos` (NO-GOs silent — never surface as questions, per D5); pre/post phase gates via `x-internal-verify-phase-gates`.
- **Phase B — Consolidate & Operator Q&A** (§B): SKIP when `--non-interactive` (set `answers = {}`). Otherwise: dedup by semantic similarity, group by 6 categories (Persona & Valor, Critérios de Aceite, Contratos & Interfaces, Métricas, Alternativas, Riscos), discard empties. **EXACTLY ONE** `AskUserQuestion` batch (D4, no per-persona loops). Parse reply into `{Qn: answer}` map.
- **Phase C — Parallel Specialist Refinement** (§C): SKIP when `--non-interactive` (set `proposedSections = {}`). Otherwise: re-launch persona agents in parallel (SIBLING calls) with each persona's own answers; collect `proposedSections` per dimension; merge into single map. Pre/post phase gates.
- **Phase D — Architect Consolidation + Dual-Write** (§D): **opus-tier** architect agent merges sections, evaluates NO-GOs vs operator answers, produces final Refinement Verdict JSON conforming to 7-dimension schema (value/persona/ac/contracts/metrics/alternatives/risks). Compute `verdictHash` (`sha256sum`). **Dual-write**: (1) `x-internal-update-status` INLINE-SKILL writes verdict to `execution-state.json` → `VERDICT_WRITE_FAILED` on non-zero; (2) Edit tool replaces/appends `## Refinement Verdict` block in story markdown with `verdictHash` populated. Final phase gate with `--expected-artifacts`.

## Output Summary

Print a concise summary:

```text
x-refine-story completed for {STORY_ID}
  Verdict: <approved|rejected|tbd>
  Blockers: <none | list>
  Verdict hash: <hash>
  State file: ai/epics/epic-XXXX/execution-state.json (field: refinementVerdict)
  Story markdown: {storyPath} (section: ## Refinement Verdict)
```

## Knowledge Pack References

Read `.claude/knowledge/refinement/dimensions.md` (§Story Dimensions) — persona-to-dimension mapping and NO-GO rules. Read `.claude/knowledge/lifecycle/refinement-gate.md` for the gate contract and CI drift detection.

## Integration Notes

- **Consumed by:** `x-implement-story` (Phase 0: checks `refinementVerdict.status == "approved"` AND `scope == "story"` via `enforce-refinement-gate.sh` hook).
- **Parallel with:** `x-refine-epic` (distinct write paths; shared `dimensions.md` KP is read-only).
- **Depends on:** `x-internal-update-status` (INLINE-SKILL, Rule 13 Pattern 1), `knowledge/refinement/dimensions.md` (KP, story-0069-0001).
- **Verdict drift detection:** `verdictHash` in state file vs `verdictHash` in story markdown is compared by `audit-refinement-gate.sh` (story-0069-0006). Drift fails CI with `REFINEMENT_VERDICT_DRIFT`.
- **Re-run idempotency:** Running `x-refine-story` again on an `approved` story replaces the verdict. This is intentional — re-refinement after story changes should update the verdict.
- **Scope discriminator:** This skill always writes `scope: "story"`. The gate `enforce-refinement-gate.sh` checks both `status == "approved"` AND `scope == "story"` to unblock `x-implement-story`. Epic-level `scope: "epic"` verdicts do NOT unblock story-level gates.

## Full Protocol

Minimum viable contract above. Detailed phase-by-phase workflow (full 7 persona `Agent()` prompts with dimension-specific NO-GO rules, telemetry sub-markers per persona, Phase B grouping categories table + dedup heuristic, Phase D architect consolidator prompt with section merge targets, verdict JSON schema + Refinement Verdict markdown format) lives in [`references/full-protocol.md`](references/full-protocol.md) per ADR-0012 (skill body slim-by-default).
