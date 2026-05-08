---
name: x-orchestrate-epic
model: sonnet
description: "Orchestrates multi-agent planning for all stories in an epic with checkpoint and resume."
user-invocable: true
allowed-tools: "Read, Write, Edit, Bash, Grep, Glob, Agent, AskUserQuestion, Skill, TaskCreate, TaskUpdate"
argument-hint: "[EPIC-ID] [--resume] [--story story-XXXX-YYYY] [--dry-run]"
requires-capabilities: []
---

## Global Output Policy

- **Language**: Use English for orchestration text, status/checkpoint messages, and control-flow markers. Use pt-BR for user-facing planning report content when required by later rules (including RULE-006).
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

# Skill: Epic Planning Orchestrator (slim — ADR-0012)

## Purpose

Orchestrate multi-agent planning for all stories in an epic by invoking `/x-plan-story` for each story in dependency order. Parse the implementation map to determine phase ordering, dispatch planning in parallel within each phase, track planning status in `execution-state.json`, and support checkpoint/resume for interrupted runs.

> **Esta skill NÃO cria épico nem stories.** Para criar a feature inteira a partir de uma spec markdown, use `/x-create-feature <SPEC-FILE-PATH> --epic-id <NNNN>`. Esta skill apenas orquestra **planning multi-agente** (Architect + QA + Security + Tech Lead + Product Owner) das stories **já criadas**.
>
> Fluxo canônico (EPIC-0065): `/x-ideate-feature` → `/x-create-feature` → `/x-orchestrate-epic`.

## Triggers

- `/x-orchestrate-epic XXXX` — plan all stories in an epic
- `/x-orchestrate-epic XXXX --resume` — resume from last checkpoint
- `/x-orchestrate-epic XXXX --story story-XXXX-YYYY` — plan only one story
- `/x-orchestrate-epic XXXX --dry-run` — write artifacts but skip Steps P1/P2/P4/P5

## Parameters

| Argument / Flag | Required | Description |
|-----------------|----------|-------------|
| `EPIC-ID` (positional) | Yes | 4-digit zero-padded id (e.g., `0028`). |
| `--resume` | No | Continue from last checkpoint (skip stories with `planningStatus == "READY"`). |
| `--story story-XXXX-YYYY` | No | Plan only the specified story (mutually exclusive with `--resume`). |
| `--dry-run` | No | Artifacts written to disk but Steps P1 / P2 / P4 / P5 become no-ops (EPIC-0049 / RULE-007). |

If `--resume` and `--story` are both provided, abort with `ERROR: --resume and --story are mutually exclusive`.

## Output Contract

| Artifact | Path | Produced By |
|----------|------|-------------|
| Execution state (checkpoint) | `{epicDir}/execution-state.json` | this skill |
| Epic planning report | `{epicDir}/reports/epic-planning-report-XXXX.md` | Phase 3 |
| Epic file update (Planning column) | `{epicDir}/EPIC-XXXX.md` | Phase 3 |
| Task breakdown (per story) | `{epicDir}/plans/tasks-story-XXXX-YYYY.md` | `/x-plan-story` subagent |
| Task plans (per task per story) | `{epicDir}/plans/task-plan-TASK-NNN-story-XXXX-YYYY.md` | `/x-plan-story` subagent |
| Planning report (per story) | `{epicDir}/plans/planning-report-story-XXXX-YYYY.md` | `/x-plan-story` subagent |
| DoR checklist (per story) | `{epicDir}/plans/dor-story-XXXX-YYYY.md` | `/x-plan-story` subagent |
| Story file update (Section 8) | `{epicDir}/story-XXXX-YYYY.md` | `/x-plan-story` subagent |

Resolve `{epicDir}` from `ai/epics/epic-{EPIC-ID}/` or its suffix variant (`epic-XXXX-title-slug/`); see Phase 0.3 in the [full protocol](references/full-protocol.md).

## CRITICAL EXECUTION RULE

**4 phases (0–3). ALL mandatory. NEVER stop before the final phase.**

After each phase 0–2: `>>> Phase N/3 completed. Proceeding to Phase N+1...`
After Phase 3: `>>> Phase 3/3 completed. Epic planning complete.`

## Workflow Overview

```
P1.  WORKTREE      -> _shared/orchestrator-prelude.md (worktree-detect)
P2.  EPIC BRANCH   -> _shared/orchestrator-prelude.md (epic-branch-ensure)
Phase 0: PREREQUISITES   -> Parse args, validate epicDir / map / stories (inline)
Phase 1: DEPENDENCY ORDER -> Read map, extract phases, order stories (inline)
Phase 2: PLAN LOOP        -> For each phase, dispatch /x-plan-story per story (subagents)
P4.  WAVE COMMIT          -> x-commit-planning per wave (after Phase 2 inner loop)
Phase 3: REPORT           -> Generate readiness summary, update epic file (inline)
P5.  PUSH                 -> x-push-branch --branch epic/<XXXX> (optional)
```

## Steps P1 + P2 — Worktree + Epic Branch Prelude

Apply the canonical [orchestrator prelude](../_shared/orchestrator-prelude.md) using
`<HOST-SKILL>` = `x-orchestrate-epic` and `<EPIC-ID>` = the resolved epic id argument.

P1 is fail-open (RULE-006 advisory). P2 aborts with `EPIC_BRANCH_ENSURE_FAILED` on non-zero exit. When `--dry-run` is set, P2 is skipped. Child `/x-plan-story` invocations in Phase 2 receive `--no-commit`; the wave-level Step P4 aggregates commits per wave.

## Phases 0–3

The detailed inline protocol for each phase (sub-step prompts, `Skill(...)` invocations, `TaskCreate`/`TaskUpdate` markers, `<!-- TELEMETRY -->` hooks, JSON schemas) lives in [`references/full-protocol.md`](references/full-protocol.md):

- **Phase 0 — Prerequisites** (§Phase 0): parse args; resolve `epicDir`; validate `IMPLEMENTATION-MAP.md` and story files; on `--resume`, reclassify state; create `plans/`; persist `interactiveMode`.
- **Phase 1 — Dependency Order** (§Phase 1): read implementation map (Sections 1 + 5); group stories by phase; in `--story` mode, validate dependencies; initialize `execution-state.json` with the schema shared with `/x-implement-epic`.
- **Phase 2 — Plan Loop** (§Phase 2): phase-by-phase dispatch of `/x-plan-story --no-commit`; per-story `subagent.start`/`subagent.end` telemetry; ≤ 3 parallel per batch; collect DoR verdict → update `storyEntries`. Step **2.5b — P4 Wave Commit** issues a single `x-commit-planning` per wave covering `execution-state.json`, story files, plans, and reports.
- **Phase 3 — Report** (§Phase 3): compute readiness summary; update epic file `Planning` column; write `reports/epic-planning-report-XXXX.md` (with RULE-002 staleness check); console summary.

## Step P5 — Push Epic Branch (optional)

After Phase 3, if `--dry-run` is not set, push the canonical `epic/<XXXX>` branch:

```text
Skill(skill: "x-push-branch", args: "--branch epic/<XXXX>")
```

On push failure log a WARNING and continue (commits preserved locally). See [full protocol §Step P5](references/full-protocol.md#step-p5--push-epic-branch-to-origin-optional-epic-0049).

## Error Envelope

> Canonical orchestrator-wide error codes (`EPIC_BRANCH_ENSURE_FAILED`, `COMMIT_FAILED`, `PR_CREATE_FAILED`, `CI_FAILED`, fail-open vs fail-closed conventions, `--dry-run` semantics) live in [`_shared/error-handling-orchestrator.md`](../_shared/error-handling-orchestrator.md). Rows below are skill-specific.

| Error | Action | Recovery |
|-------|--------|----------|
| Epic directory not found | Abort with error message | Run `/x-create-feature` or `/x-internal-create-epic` first |
| `IMPLEMENTATION-MAP.md` not found | Abort with error message | Run `/x-internal-map-epic` first |
| No story files found | Abort with error message | Run `/x-internal-create-story` first |
| `execution-state.json` missing on `--resume` | Abort with error message | Run without `--resume` |
| `--resume` and `--story` both set | Abort with error message | Use only one flag |
| Story not in implementation map (`--story`) | Abort with error message | Check story ID |
| Dependencies not satisfied (`--story`) | Abort with error message | Plan dependencies first |
| Subagent fails for a story | Mark `NOT_READY`, continue with siblings | Fix issues, re-run with `--resume` |
| DoR verdict cannot be extracted | Mark `NOT_READY`, log warning | Inspect subagent output manually |

## Checkpoint / Resume

`execution-state.json` is the checkpoint file; updated atomically after each story. On `--resume`, READY stories are skipped, IN_PROGRESS / NOT_READY are reset to PENDING. Schema is shared with `/x-implement-epic` (`stories[]`, `baseBranch`, `totalPhases` plus per-story `storyEntries`). Full reclassification table and resume rules: [`references/full-protocol.md` §Checkpoint/Resume](references/full-protocol.md#checkpoint--resume-protocol).

## Full Protocol

Minimum viable contract above. Detailed phase-by-phase workflow, JSON schemas, telemetry hooks, file naming convention (RULE-004), integration contract with `/x-implement-epic`, and runtime constraints (memory, atomicity, staleness, content language) live in [`references/full-protocol.md`](references/full-protocol.md) per ADR-0012 (skill body slim-by-default).
