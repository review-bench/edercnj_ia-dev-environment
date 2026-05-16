---
name: x-plan-task
description: "Generates plan-task-*.md with TDD cycles in TPP order, file-impact, and exit criteria."
user-invocable: true
allowed-tools: Read, Write, Edit, Bash, Grep, Glob, Skill
argument-hint: "--task-file <path> [--output-dir <dir>] [--no-commit] [--dry-run]  |  [STORY-ID] --task [TASK-ID] [--force] [--no-commit] [--dry-run]"
context-budget: heavy
requires-capabilities: []
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

# Skill: Plan Task (slim — ADR-0012)

## Purpose

Produces a detailed narrative implementation plan for a single task. The plan is a self-contained execution guide readable by a human auditor without consulting other documents. It contains: back-reference to the parent `plan-story-XXXX-YYYY.md` section, objective in plain language, class/component diagram scoped to this task, mapping of all existing tests impacted by this task, verbal description of what to write/change in each file, TDD cycles in TPP order described in natural language (RED/GREEN/REFACTOR), security checklist, file footprint, and explicit completion criteria (including how to mark the task as Concluída and update `execution-state.json`).

## Triggers

- `/x-plan-task --task-file <path>` — **task-file-first** (EPIC-0038): consume a standalone `task-TASK-XXXX-YYYY-NNN.md` contract, write the plan next to it.
- `/x-plan-task --task-file <path> --output-dir <dir>` — override output directory.
- `/x-plan-task --task-file <path> --no-commit` — **batch mode** (EPIC-0049 story-0049-0017): write the plan file but SKIP the individual planning-commit. Callers (e.g., `x-plan-story`) aggregate N plans and issue ONE batched commit.
- `/x-plan-task STORY-ID --task TASK-ID` — **story-scoped (legacy)**: read task from story Section 8.
- `/x-plan-task STORY-ID --task TASK-ID --force` — regenerate even if plan exists.
- `/x-plan-task STORY-ID --task TASK-ID --no-commit` — **batch mode (story-scoped)**: write plan but skip commit.

> **Invocation modes.** Task-file-first is the canonical path: an orchestrator (human or `x-plan-story`) generates `task-TASK-NNN.md` files and pipes each one through this skill. Story-scoped mode also accepted when tasks are declared as sub-sections of the story file. See [full protocol §1](references/full-protocol.md#1-invocation-modes-detailed) for the validation rules of each mode.

## Parameters

### Task-file-first mode (EPIC-0038)

| Parameter | Required | Description |
|-----------|----------|-------------|
| `--task-file` | Yes | Path to a `task-TASK-XXXX-YYYY-NNN.md` file (schema: story-0038-0001). MUST pass `TaskFileParser` validation. |
| `--output-dir` | No (default: same dir as `--task-file`) | Directory to write `plan-task-TASK-XXXX-YYYY-NNN.md`. |
| `--force` | No | Regenerate plan even if a fresh one already exists. |
| `--no-commit` | No (default: `false`) | When `true`, skip the Planning Status Propagation commit (Phase 5.4). Plan file is still written to disk and status is still flipped `Pendente -> Planejada`, but the commit step is deferred to the caller. Used by `x-plan-story` to batch-commit N plans in a single commit (EPIC-0049 story-0049-0017). |
| `--dry-run` | No (default: `false`) | When `true`, plan file is written to disk but Steps P2 / P4 / P5 (branch-ensure / planning-commit / push) become no-ops (EPIC-0049 / RULE-007). |

### Story-scoped mode (legacy — epics 0025-0037)

| Parameter | Required | Description |
|-----------|----------|-------------|
| `STORY-ID` | Yes | Story identifier (pattern: `story-XXXX-YYYY`). |
| `--task` | Yes | Task identifier (pattern: `TASK-XXXX-YYYY-NNN`). Must exist in story Section 8. |
| `--force` | No | Regenerate plan even if a fresh one already exists. |
| `--no-commit` | No (default: `false`) | Same semantics as in task-file-first mode: write plan, skip commit; caller handles batched commit. |
| `--dry-run` | No (default: `false`) | Same semantics as in task-file-first mode: Steps P2 / P4 / P5 become no-ops. |

## Output Contract

| Artifact | Path | Produced By |
|----------|------|-------------|
| Task plan | `<EPIC_DIR>/plans/plan-task-TASK-XXXX-YYYY-NNN.md` | Phase 5 (Write Plan) |
| Status update on task file | `Pendente → Planejada` | Phase 5.4 (Planning Status Propagation, v2 only) |
| Planning commit (batched or single) | `epic/<XXXX>` ref | Phase 5.4 / Step P4 (suppressed in `--no-commit` / `--dry-run`) |
| Push to origin (optional) | `epic/<XXXX>` ref | Step P5 (suppressed in `--no-commit` / `--dry-run`) |

## CRITICAL EXECUTION RULE

**5 phases (0–5) framed by P1+P2 prelude and P5 epilogue. Phase 5.4 doubles as Step P4. ALL mandatory in default mode. NEVER stop before Phase 5.**

## Workflow Overview

```
P1.  WORKTREE         -> _shared/orchestrator-prelude.md (worktree-detect)
P2.  EPIC BRANCH      -> _shared/orchestrator-prelude.md (epic-branch-ensure)
Phase 0: VALIDATE & PRE-CHECK -> args, epic-dir resolution, staleness check (inline)
Phase 1: EXTRACT CONTRACTS    -> task-file branch (1A) or story-scoped branch (1B)
Phase 2: MAP TDD CYCLES       -> TPP-ordered cycles in natural language
Phase 3: ANALYZE FILES        -> Organize affected files by architecture layer
Phase 4: SECURITY CHECKLIST   -> Task-type-aware items with CWE/OWASP refs
Phase 4.5: FILE FOOTPRINT     -> Structured write:/read:/regen: block
Phase 5: WRITE PLAN           -> Assemble + write plan-task-*.md per template
  └ 5.4 PLANNING STATUS PROPAGATION (= Step P4)  -> flip task status + batched commit
P5.  PUSH             -> x-push-branch --branch epic/<XXXX> (optional)
```

## Steps P1 + P2 — Worktree + Epic Branch Prelude

Apply the canonical [orchestrator prelude](../_shared/orchestrator-prelude.md) using
`<HOST-SKILL>` = `x-plan-task` and `<EPIC-ID>` resolved from the task source:

- **Task-file mode (`--task-file`):** extract `XXXX` from the task-file path (`ai/epics/epic-XXXX/plans/task-TASK-XXXX-YYYY-NNN.md`) or from the task's `**Task ID:**` header.
- **Story-scoped mode:** extract `XXXX` from the `STORY-ID` argument (e.g., `story-0049-0001` → `0049`).

P1 is fail-open (RULE-006 advisory). P2 aborts with `EPIC_BRANCH_ENSURE_FAILED` on non-zero exit. P2 is skipped on `--no-commit` or `--dry-run` per the canonical prelude contract.

## Phases 0–5

The detailed inline protocol for each phase (validation rules, TPP cycle structure, mandatory plan template, security checklist by task type, File Footprint inference rules, Planning Status Propagation CLI commands) lives in [`references/full-protocol.md`](references/full-protocol.md):

- **Phase 0 — Validate & Pre-Check** (§Phase 0): parse args, resolve `EPIC_DIR` (exact or suffix variant), resolve paths, and run the staleness check (`mtime` comparison) — skip regeneration when plan is fresh and `--force` not set.
- **Phase 1 — Extract Contracts** (§Phase 1): two branches — **1A task-file-first** (validate per `task-schema.md`, project into `TaskContract`); **1B story-scoped legacy** (locate task heading in `## 8. Tasks` of the story file, extract fields).
- **Phase 2 — Map TDD Cycles (TPP Order)** (§Phase 2): generate 3–6 cycles in strict Transformation Priority Premise order; Cycle 1 MUST be degenerate; each cycle described in natural language (RED test, GREEN minimal impl, REFACTOR, commit subject).
- **Phase 3 — Analyze Affected Files by Layer** (§Phase 3): organize files in Domain → Port → Adapter → Application → Config → Test order; mark `CREATE`/`MODIFY`.
- **Phase 4 — Security Checklist** (§Phase 4): task-type-aware items (Endpoint/API, Persistence/DB, Domain Logic, Config, Integration) with CRITICAL/HIGH/MEDIUM severity and CWE/OWASP refs.
- **Phase 4.5 — Compute File Footprint** (§Phase 4.5): emit machine-readable `## File Footprint` block (`write:`/`read:`/`regen:` sub-sections, alphabetically sorted, empty sub-sections omitted) for downstream `/x-evaluate-parallelism` consumption.
- **Phase 5 — Write Plan** (§Phase 5): assemble and write `plan-task-TASK-XXXX-YYYY-NNN.md` per `_TEMPLATE-TASK-PLAN.md`; prepend MANDATORY EPIC-0059 origin marker frontmatter; sections 1–8 with `OBRIGATÓRIO` markers per template.
- **Phase 5.4 — Planning Status Propagation** (§Planning Status Propagation, alias of Step P4): v2-gated `Pendente → Planejada` status flip via `StatusFieldParserCli`; commit gated by `--no-commit`.

## Step P4 — Planning Status Commit (alias)

The planning-commit step is performed inside **Phase 5.4**. When `--no-commit=true` or `--dry-run=true`, that step becomes a no-op (`"[no-commit] Plan written; commit deferred to caller"` or `"dry-run, skipping commit"`). No additional P4 invocation is issued; this alias exists so the P1–P5 convention is readable end-to-end. See [full protocol §Step P4](references/full-protocol.md#step-p4--planning-status-commit-alias-of-phase-54) for the rationale.

## Step P5 — Push to Origin (optional)

If `--dry-run` or `--no-commit` is set, skip.

Otherwise delegate to `x-push-branch`:

```text
Skill(skill: "x-push-branch", args: "--branch epic/<XXXX>")
```

On push failure, log a WARNING and continue — local commit is preserved. See [full protocol §Step P5](references/full-protocol.md#step-p5--push-to-origin-optional-epic-0049).

## Error Envelope

> Canonical orchestrator-wide error codes (`EPIC_BRANCH_ENSURE_FAILED`, `COMMIT_FAILED`, fail-open vs fail-closed conventions, `--dry-run` semantics) live in [`_shared/error-handling-orchestrator.md`](../_shared/error-handling-orchestrator.md). Rows below are skill-specific.

### Task-file-first mode (EPIC-0038)

| Scenario | Exit Code | Message |
|----------|-----------|---------|
| Task file missing or unreadable | 1 | `Task file invalid: file not found at {path}` |
| Task file schema violations (story-0038-0001) | 1 | `Task file invalid: {violations}` |
| Testability not declared (§2.3 empty / multiple checked) | 3 | `Testability not declared (RULE-TF-01). Declare Testability: Independent OR Requires Mock OR Coalesced` |
| Output dir not writable | 2 | `Output dir not writable: {path}` |
| Plan generated | 0 | `Plan written to {path}` |
| Plan exists and fresh (no `--force`) | 0 | `Task plan already exists and is up-to-date` |

### Story-scoped mode (legacy)

| Scenario | Action |
|----------|--------|
| No story ID provided | Prompt: `"Usage: /x-plan-task [STORY-ID] --task [TASK-ID] [--force]"` |
| No task ID provided | Abort: `"--task flag is required. Provide a TASK-XXXX-YYYY-NNN identifier."` |
| Story file not found | Abort: `"Story file not found at {path}"` |
| Section 8 not found | Abort: `"Section 8 (Tasks) not found in story {story-id}"` |
| Task ID not in Section 8 | Abort: `"Task {task-id} not found in story {story-id} Section 8"` |
| Plan exists and fresh | Return existing: `"Task plan already exists and is up-to-date"` |
| Plan exists with --force | Regenerate: `"Regenerating task plan (--force)"` |
| Epic directory not found | Abort: `"Epic directory not found for epic-XXXX"` |
| `x-internal-ensure-epic-branch` fails (Step P2) | Abort with `EPIC_BRANCH_ENSURE_FAILED`; canonical branch is required for versioning |
| `x-push-branch` fails (Step P5) | WARN only; local commit preserved; operator re-runs manually |
| `--dry-run` set | Steps P2, P4 (Phase 5.4) and P5 become no-ops |
| `--no-commit` set | Steps P2, P4 (Phase 5.4) and P5 become no-ops — orchestrator owns branch + commit lifecycle |

## Integration Notes

| Skill | Relationship | Context |
|-------|-------------|---------|
| `x-plan-story` | complementary | x-plan-story generates task breakdown; x-plan-task generates per-task execution plans |
| `x-implement-story` | called-by | Phase 2 (PRE_PLANNED mode) reads task plans to drive implementation |
| `x-implement-task` | consumed-by | Task plans serve as implementation guides for the developer |
| `x-plan-tests` | complementary | x-plan-tests covers story-level tests; x-plan-task maps per-task TDD cycles |
| `x-manage-worktrees` | calls (Step P1) | Detect-context (EPIC-0049 / RULE-001) |
| `x-internal-ensure-epic-branch` | calls (Step P2) | Ensure `epic/<ID>` exists locally + origin (EPIC-0049 / RULE-001) |
| `x-commit-changes` | calls (Phase 5.4 / Step P4) | Commit plan + status flip in standalone mode |
| `x-push-branch` | calls (Step P5) | Push canonical epic branch to origin (optional) |

## Knowledge Pack References

| Pack | Files | Purpose |
|------|-------|---------|
| testing | `knowledge/testing.md` | TDD patterns, TPP levels, test naming conventions |
| architecture | `knowledge/architecture.md` | Layer definitions, package structure, dependency rules |
| security | `knowledge/security/index.md` | OWASP Top 10, security checklist items |
| parallelism-heuristics | `knowledge/parallelism-heuristics.md` | File Footprint semantics (write/read/regen sub-sections) consumed by Phase 4.5 |
| coding-standards | `knowledge/coding-standards.md` | {{LANGUAGE}} conventions, naming, SOLID principles |

## Anti-Patterns

- Do NOT write implementation code — only plan the approach
- Do NOT skip degenerate cases in TDD cycles (Cycle 1 is ALWAYS degenerate)
- Do NOT generate security items unrelated to the task type
- Do NOT list files outside the task scope
- Do NOT include cycles beyond what the task complexity requires (CRUD tasks need 3-4 cycles, not 8)
- Do NOT ignore the task's layer assignment — files must align with the declared layer
- Do NOT generate cycles in non-TPP order (never start with complex cases)

## Full Protocol

Minimum viable contract above. Detailed phase-by-phase workflow (validation rules, mandatory plan template with all OBRIGATÓRIO sections, TPP cycle structure, security checklist by task type, File Footprint inference rules R1–R5, EPIC-0059 origin-marker frontmatter, Rule 22 Planning Status Propagation with `StatusFieldParserCli` invocations, P4/P5 telemetry markers) lives in [`references/full-protocol.md`](references/full-protocol.md) per ADR-0012 (skill body slim-by-default).
