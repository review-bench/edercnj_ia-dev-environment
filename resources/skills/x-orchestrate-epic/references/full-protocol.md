# `x-orchestrate-epic` — Full Protocol

> Full protocol details for the epic planning orchestrator. The slim
> [`SKILL.md`](../SKILL.md) carries the minimum viable behavioral contract
> (per ADR-0012). This document is loaded on-demand when the operator (or the
> LLM acting on the skill's behalf) needs the verbose phase-by-phase detail,
> the checkpoint/resume protocol, or the integration contract with
> `/x-implement-epic`.

## Phase 0 — Prerequisites (Orchestrator — Inline)

```text
TaskCreate(subject: "{epicId} › Phase 0 - Prerequisites", activeForm: "Validating epic prerequisites")
```

Close with `TaskUpdate(id: phase0TaskId, status: "completed")` after step 0.7.

### 0.1 Parse Epic ID

Extract the 4-digit zero-padded epic ID from the positional argument.

- Input `0028` → epic ID = `0028`.
- If the argument is not a 4-digit number, abort with format error:

  ```
  ERROR: Invalid epic ID format. Expected 4-digit zero-padded number (e.g., 0028).
  ```

### 0.2 Parse Flags

Check for `--resume` and `--story` flags. Validate mutual exclusivity (see
SKILL.md §Parameters).

### 0.3 Resolve Epic Directory

Resolve the epic directory using a glob to support suffix variants
(e.g., `ai/epics/epic-XXXX-title-slug/`):

```bash
epicDir=$(ls -d ai/epics/epic-{epicId}/ ai/epics/epic-{epicId}-*/ 2>/dev/null | head -1)
```

If no match is found, abort:

```
ERROR: Directory ai/epics/epic-{epicId}/ (or suffix variant) not found. Run /x-create-feature <SPEC-FILE-PATH> --epic-id {epicId} first.
```

Use the resolved `epicDir` path for ALL subsequent reads/writes
(IMPLEMENTATION-MAP.md, stories, execution-state.json, reports).

### 0.4 Validate Implementation Map

Check that `IMPLEMENTATION-MAP.md` exists in the epic directory.

```
ERROR: IMPLEMENTATION-MAP.md not found in ai/epics/epic-{epicId}/. Run /x-create-feature first (it delegates internally to x-internal-map-epic).
```

### 0.5 Validate Story Files

Glob for `story-XXXX-*.md` files in the epic directory. At least one must
exist.

```
ERROR: No story files found matching story-{epicId}-*.md in ai/epics/epic-{epicId}/.
```

### 0.6 Resume Checkpoint Validation (Conditional)

If `--resume` flag is set, check that `execution-state.json` exists in the
epic directory.

```
ERROR: No checkpoint found (execution-state.json missing). Cannot resume. Run without --resume.
```

If `--resume` is set and `execution-state.json` exists, load it and apply
reclassification:

| Current `planningStatus` | New `planningStatus` | Rationale |
|--------------------------|----------------------|-----------|
| `READY` | `READY` | Preserved — skip this story |
| `NOT_READY` | `PENDING` | Re-attempt planning |
| `IN_PROGRESS` | `PENDING` | Interrupted — reset for retry |
| `PENDING` | `PENDING` | No change |

### 0.7 Create Plans Directory

Create `{epicDir}/plans/` if it does not exist:

```bash
mkdir -p {epicDir}/plans
```

Log `"Created plans/ directory for EPIC-{epicId}"` if created. Skip silently
if already exists.

Persist `interactiveMode` to `execution-state.json` (EPIC-0068 — consumed by
the Stop hook `enforce-continuous-flow.sh`):

```text
Skill(skill: "x-internal-update-status",
      args: "--file {epicDir}/execution-state.json --type epic --id <EPIC-ID> --field interactiveMode --value <interactive|non-interactive>")
```

Value: `"interactive"` when `--interactive` passed; otherwise
`"non-interactive"` (Rule 20 default).

```text
TaskUpdate(id: phase0TaskId, status: "completed")
```

`>>> Phase 0/3 completed. Proceeding to Phase 1...`

---

## Phase 1 — Dependency Order (Orchestrator — Inline)

```text
Skill(skill: "x-internal-verify-phase-gates", model: "haiku", args: "--mode pre --skill x-orchestrate-epic --phase Phase-1-Discovery")
TaskCreate(subject: "{epicId} › Phase 1 - Discovery", activeForm: "Resolving story dependency order")
```

```text
<!-- TELEMETRY: phase.start -->
Bash command: $CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start x-orchestrate-epic Phase-1-Discovery
```

### 1.1 Read Implementation Map

Read `{epicDir}/IMPLEMENTATION-MAP.md` and extract phases and dependencies
from two sections:

1. **Dependency declarations** from **Section 1 — Dependency Matrix**:

   ```markdown
   | Story | Título | Chave Jira | Blocked By | Blocks | Status |
   | :--- | :--- | :--- | :--- | :--- | :--- |
   | story-XXXX-0001 | Title | KEY-1 | — | story-XXXX-0002 | Pendente |
   | story-XXXX-0002 | Title | KEY-2 | story-XXXX-0001 | — | Pendente |
   ```

   Parse the `Blocked By` and `Blocks` columns to build the dependency graph.

2. **Phase assignments** from **Section 5 — Resumo por Fase**:

   ```markdown
   | Fase | Histórias | Camada | Paralelismo | Pré-requisito |
   | :--- | :--- | :--- | :--- | :--- |
   | 0 | story-XXXX-0001 | Domain | 1 paralela | — |
   | 1 | story-XXXX-0002, story-XXXX-0003 | Application | 2 paralelas | Fase 0 concluída |
   | 2 | story-XXXX-0004 | Adapter | 1 paralela | Fase 1 concluída |
   ```

   Parse the `Fase` and `Histórias` columns to determine phase grouping.
   Extract total phases from the number of distinct `Fase` values.

### 1.2 Order Stories by Phase

1. Group stories by their phase number.
2. Sort phases numerically: Phase 0, Phase 1, Phase 2, …
3. Within each phase, stories can be planned in **parallel** (no
   inter-phase dependencies within the same phase).

### 1.3 Single-Story Mode (Conditional)

If `--story` flag is provided:

1. Locate the specified story in the implementation map.
2. If not found, abort:

   ```
   ERROR: Story {storyId} not found in implementation map.
   ```

3. Identify the story's dependencies from the implementation map.
4. Validate that all dependencies have `planningStatus == "READY"` in
   `execution-state.json`:
   - If `execution-state.json` does not exist, check whether dependency
     story files have existing planning artifacts (tasks file at
     `{epicDir}/plans/tasks-story-XXXX-YYYY.md`).
   - If any dependency is not satisfied, abort:

     ```
     ERROR: Dependencies not satisfied for {storyId}: [{unsatisfied deps list}]. Plan dependencies first.
     ```

5. If all dependencies are satisfied, set the plan loop to process only
   this story.

### 1.4 Initialize Execution State

If `execution-state.json` does not exist in `{epicDir}/`, create it using
the same top-level schema as `/x-implement-epic`, adding planning-specific
fields under each story entry:

```json
{
  "epicId": "XXXX",
  "baseBranch": "develop",
  "startedAt": "{ISO-8601 timestamp}",
  "totalPhases": 0,
  "stories": [
    {"id": "story-XXXX-0001", "phase": 0},
    {"id": "story-XXXX-0002", "phase": 1}
  ],
  "storyEntries": {
    "story-XXXX-0001": {
      "id": "story-XXXX-0001",
      "phase": 0,
      "planningStatus": "PENDING",
      "planningStartedAt": null,
      "planningCompletedAt": null,
      "dorVerdict": null,
      "artifacts": []
    }
  }
}
```

**Schema compatibility with `/x-implement-epic`.** The `stories` array and
`baseBranch` field match the schema expected by `/x-implement-epic`.
Planning-specific fields (`planningStatus`, `dorVerdict`, `artifacts`) are
added under `storyEntries` alongside where `/x-implement-epic` adds its own
fields (`status`, `commitSha`, `prUrl`, etc.). Both skills read/write the
same file without conflict.

If `execution-state.json` already exists (either from `--resume` or a prior
partial run), preserve existing data and only add `storyEntries` for stories
not yet tracked. Never overwrite fields owned by `/x-implement-epic`.

**Per-story planning fields in `storyEntries`:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `id` | String | Yes | Story ID (e.g., `story-0028-0001`) |
| `phase` | Integer | Yes | Phase number from implementation map |
| `planningStatus` | String | Yes | `PENDING`, `IN_PROGRESS`, `READY`, `NOT_READY` |
| `planningStartedAt` | String | When IN_PROGRESS | ISO-8601 timestamp of planning start |
| `planningCompletedAt` | String | When READY/NOT_READY | ISO-8601 timestamp of planning completion |
| `dorVerdict` | String | When completed | `READY` or `NOT_READY` from `/x-plan-story` output |
| `artifacts` | String[] | When completed | List of generated artifact paths |

Log the execution plan:

```
Epic Planning -- EPIC-{epicId}
Total stories: {count}
Total phases: {count}
Mode: {all | resume | single-story}
Execution order:
  Phase 0: story-XXXX-0001
  Phase 1: story-XXXX-0002, story-XXXX-0003
  Phase 2: story-XXXX-0004
```

```text
<!-- TELEMETRY: phase.end -->
Bash command: $CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end x-orchestrate-epic Phase-1-Discovery ok
```

```text
Skill(skill: "x-internal-verify-phase-gates", model: "haiku", args: "--mode post --skill x-orchestrate-epic --phase Phase-1-Discovery --expected-artifacts ai/epics/epic-{epicId}/execution-state.json")
TaskUpdate(id: phase1TaskId, status: "completed")
```

`>>> Phase 1/3 completed. Proceeding to Phase 2...`

---

## Phase 2 — Plan Loop (Orchestrator — Dispatches Subagents)

```text
Skill(skill: "x-internal-verify-phase-gates", model: "haiku", args: "--mode pre --skill x-orchestrate-epic --phase Phase-2-PlanLoop")
TaskCreate(subject: "{epicId} › Phase 2 - Plan Loop", activeForm: "Orchestrating story planning loop")
```

```text
<!-- TELEMETRY: phase.start -->
Bash command: $CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start x-orchestrate-epic Phase-2-Story-Orchestration
```

Execute planning for each story in dependency order, phase by phase.

### 2.1 Phase-by-Phase Execution

For each phase (0..N), in order:

```
For each phase in (0..totalPhases-1):
  1. Collect all stories in this phase
  2. Filter out stories with planningStatus == "READY" (already planned)
  3. For each remaining story in this phase:
     a. Update execution-state.json: planningStatus = "IN_PROGRESS", planningStartedAt = now()
     b. Dispatch /x-plan-story via Agent subagent
     c. Collect DoR verdict from subagent result
     d. Update execution-state.json with result
  4. Log phase completion summary
```

### 2.2 Subagent Dispatch

> **CONTEXT ISOLATION.** You receive only metadata. Read all files yourself.
> Do NOT expect source code, diffs, or knowledge pack content in this prompt.
> The subagent reads all story files, KPs, and references independently.

For each story to plan, invoke `/x-plan-story` via the Skill tool.

**Telemetry around per-story dispatch (story-0040-0007 §3.2).** Before each
subagent invocation emit a `subagent.start` marker with the story ID as the
role; after the subagent returns emit a `subagent.end` marker carrying the
outcome status. The role value is the full story ID (e.g.,
`story-0028-0001`) so telemetry analysis can group per-story planning
latency:

```text
<!-- TELEMETRY: subagent.start -->
Bash command: $CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh subagent-start x-orchestrate-epic {storyId}
```

**Skill invocation:**

```text
Skill(skill: "x-plan-story", args: "{storyId} --no-commit")
```

`--no-commit` is always propagated (EPIC-0049 / RULE-007 batch-commit
contract). Each child `x-plan-story` writes artifacts to disk but SKIPS its
own P4 commit. The wave-level **Step P4 — Commit Wave Artifacts** (below,
added to Phase 2.5) aggregates every story's artifacts plus
`execution-state.json` plus reports into a single commit per wave.

```text
<!-- TELEMETRY: subagent.end -->
Bash command: $CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh subagent-end x-orchestrate-epic {storyId} ok
```

The skill executes all 6 phases of `/x-plan-story` (Input Resolution,
Context Gathering, Parallel Planning, Consolidation, Artifact Generation,
DoR Validation) and returns a result containing the DoR verdict.

If `/x-plan-story` is unavailable via the Skill tool, fall back to the
Agent tool:

```text
Agent(prompt: "/x-plan-story {storyId} --no-commit")
```

**Parallel dispatch within a phase.** Stories within the same phase have no
inter-dependencies, so they CAN be dispatched in parallel using multiple
Agent calls in a single message. To manage context window and memory
pressure:

- If the phase contains ≤ 3 stories: dispatch all in parallel (single
  message with multiple Agent calls).
- If the phase contains > 3 stories: dispatch in batches of 3.

### 2.3 Collect Results

After each subagent completes, extract the DoR verdict from the output:

1. Search the subagent output for the DoR verdict line:
   `Verdict: **READY**` or `Verdict: **NOT_READY**`.
2. If the verdict cannot be extracted, check for the existence of the DoR
   file at `{epicDir}/plans/dor-story-XXXX-YYYY.md` and read the verdict
   from it.
3. If neither source provides a verdict, set
   `planningStatus = "NOT_READY"` and log a warning:

   ```
   WARNING: Could not extract DoR verdict for {storyId}. Marking as NOT_READY.
   ```

### 2.4 Update Execution State

After each story's planning completes, update the story's entry in
`storyEntries`:

```json
{
  "id": "story-XXXX-YYYY",
  "phase": 0,
  "planningStatus": "READY",
  "planningStartedAt": "2026-04-07T10:00:00Z",
  "planningCompletedAt": "2026-04-07T10:05:00Z",
  "dorVerdict": "READY",
  "artifacts": [
    "{epicDir}/plans/tasks-story-XXXX-YYYY.md",
    "{epicDir}/plans/planning-report-story-XXXX-YYYY.md",
    "{epicDir}/plans/dor-story-XXXX-YYYY.md"
  ]
}
```

Map DoR verdict to planningStatus:

| DoR Verdict | planningStatus |
|-------------|----------------|
| `READY` | `READY` |
| `NOT_READY` | `NOT_READY` |
| Error / timeout | `NOT_READY` |

Write `execution-state.json` atomically after each story update. This
ensures checkpoint consistency even if the process is interrupted.

### 2.5 Phase Completion Log

After all stories in a phase are planned:

```
Phase {N} planning complete:
  story-XXXX-0001: READY
  story-XXXX-0002: NOT_READY (3 blockers)
  Phase {N} result: {passed}/{total} stories READY
```

### 2.5b Step P4 — Commit Wave Planning Artifacts (EPIC-0049 / RULE-007)

```text
<!-- TELEMETRY: phase.start -->
Bash command: $CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start x-orchestrate-epic Phase-P4-Wave-Commit
```

If `--dry-run` is set, log `"dry-run, skipping commit"` and skip this step.

After all stories in the current wave (phase) have completed planning and
`execution-state.json` has been updated, issue a **single consolidated
commit** covering:

1. `execution-state.json` (wave-level checkpoint update).
2. Every story file touched during the wave (Section 8 updates via status
   flip).
3. Every planning artifact produced by the wave's stories:
   - `ai/epics/epic-XXXX/plans/tasks-story-XXXX-YYYY.md`
   - `ai/epics/epic-XXXX/plans/planning-report-story-XXXX-YYYY.md`
   - `ai/epics/epic-XXXX/plans/dor-story-XXXX-YYYY.md`
   - `ai/epics/epic-XXXX/plans/plan-story-XXXX-YYYY.md` (if present)
   - `ai/epics/epic-XXXX/plans/task-TASK-*.md` (v2)
   - `ai/epics/epic-XXXX/plans/plan-task-TASK-*.md` (v2)
   - `ai/epics/epic-XXXX/plans/task-implementation-map-STORY-*.md` (v2)
4. Any reports written during the wave
   (`ai/epics/epic-XXXX/reports/**` — added in Phase 3 but may accumulate
   incrementally).

Delegate to `x-commit-planning`:

```text
Skill(skill: "x-commit-planning",
      args: "--scope chore --epic-id <XXXX> --paths ai/epics/epic-<XXXX>/execution-state.json ai/epics/epic-<XXXX>/story-<XXXX>-*.md ai/epics/epic-<XXXX>/plans/ ai/epics/epic-<XXXX>/reports/ --subject \"planning orchestration cycle (wave <N>)\"")
```

Where `<N>` is the current phase number (0-based per Phase 1 — or the wave
sequence number when a multi-wave orchestration runs phase-by-phase).

Cenário enforced (story-0049-0022): `"1 commit 'chore(epic-<XXXX>): planning
orchestration cycle (wave <N>)' é criado"` per completed wave.

Idempotency: re-executing with identical inputs produces `commitSha=null`
(silent no-op). On `COMMIT_FAILED` (exit 4), abort with the same code (see
[`error-handling-orchestrator.md`](../../_shared/error-handling-orchestrator.md)).

```text
<!-- TELEMETRY: phase.end -->
Bash command: $CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end x-orchestrate-epic Phase-P4-Wave-Commit ok
```

### 2.6 Error Handling

If a subagent fails (throws error, times out, or returns no output):

1. Set `planningStatus = "NOT_READY"` for the story.
2. Set `dorVerdict = null`.
3. Log the error:

   ```
   ERROR: Planning failed for {storyId}: {error message}
   ```

4. Continue with the next story (do NOT abort the entire epic planning).

Stories in subsequent phases that depend on a `NOT_READY` story are still
planned (planning is about generating artifacts, not about runtime
dependencies). The `NOT_READY` status is informational — it indicates the
story may need attention before implementation begins.

```text
<!-- TELEMETRY: phase.end -->
Bash command: $CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end x-orchestrate-epic Phase-2-Story-Orchestration ok
```

```text
Skill(skill: "x-internal-verify-phase-gates", model: "haiku", args: "--mode post --skill x-orchestrate-epic --phase Phase-2-PlanLoop --expected-artifacts ai/epics/epic-{epicId}/execution-state.json")
TaskUpdate(id: phase2TaskId, status: "completed")
```

`>>> Phase 2/3 completed. Proceeding to Phase 3...`

---

## Phase 3 — Report (Orchestrator — Inline)

```text
Skill(skill: "x-internal-verify-phase-gates", model: "haiku", args: "--mode pre --skill x-orchestrate-epic --phase Phase-3-Report")
TaskCreate(subject: "{epicId} › Phase 3 - Report", activeForm: "Generating epic planning report")
```

```text
<!-- TELEMETRY: phase.start -->
Bash command: $CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start x-orchestrate-epic Phase-3-Consolidation
```

### 3.1 Generate Readiness Summary

Read the final `execution-state.json` and compute:

| Metric | Calculation |
|--------|-------------|
| `stories_planned` | Count of stories with `planningStatus != "PENDING"` |
| `stories_total` | Total number of stories in the epic |
| `stories_ready` | Count of stories with `planningStatus == "READY"` |
| `stories_not_ready` | Count of stories with `planningStatus == "NOT_READY"` |
| `stories_pending` | Count of stories with `planningStatus == "PENDING"` |
| `overall_status` | See determination table below |

**Overall status determination:**

| Condition | `overall_status` |
|-----------|------------------|
| All stories have `planningStatus == "READY"` | `READY` |
| At least one story is `READY` and at least one is `NOT_READY` or `PENDING` | `PARTIALLY_READY` |
| No stories are `READY` | `NOT_READY` |

### 3.2 Update Epic File

Read the epic file at `{epicDir}/EPIC-XXXX.md` (or `{epicDir}/epic-XXXX.md`
— case-insensitive glob).

Locate the story index table in Section 5 (or the main story listing). Add
or update a `Planning` column:

```markdown
| # | Story ID | Title | Status | Planning |
|---|----------|-------|--------|----------|
| 1 | story-XXXX-0001 | Title | Draft | READY |
| 2 | story-XXXX-0002 | Title | Draft | NOT_READY |
| 3 | story-XXXX-0003 | Title | Draft | PENDING |
```

**Rules for updating the table:**

1. If a `Planning` column already exists, update the values.
2. If no `Planning` column exists, add it as the last column.
3. Preserve all existing columns and data.
4. Only update rows for stories that were planned in this run (preserve
   existing values for stories not in scope).

### 3.3 Generate Planning Report

Write a planning summary to
`{epicDir}/reports/epic-planning-report-XXXX.md`:

```markdown
# Epic Planning Report -- EPIC-{epicId}

> **Epic ID:** EPIC-{epicId}
> **Date:** {currentDate}
> **Total Stories:** {stories_total}
> **Stories Planned:** {stories_planned}
> **Overall Status:** {overall_status}

## Readiness Summary

| Metric | Count |
|--------|-------|
| Stories Total | {stories_total} |
| Stories Planned | {stories_planned} |
| Stories Ready (DoR READY) | {stories_ready} |
| Stories Not Ready (DoR NOT_READY) | {stories_not_ready} |
| Stories Pending | {stories_pending} |

## Per-Story Results

| # | Story ID | Phase | Planning Status | DoR Verdict | Duration |
|---|----------|-------|-----------------|-------------|----------|
| 1 | story-XXXX-0001 | 0 | READY | READY | 45s |
| 2 | story-XXXX-0002 | 1 | NOT_READY | NOT_READY | 38s |

## Blockers (if any)

{List stories with NOT_READY verdict and their failing DoR checks, if available from the DoR checklist files}

## Generated Artifacts

{List all artifact files generated during this planning run}
```

### 3.4 Staleness Check for Report (RULE-002)

Before generating the planning report:

1. Compute report path:
   `{epicDir}/reports/epic-planning-report-XXXX.md`.
2. Check if the report file exists:
   - If NOT found: generate new. Log
     `"Generating planning report for EPIC-{epicId}"`.
   - If found, compare modification times:
     - `mtime(execution-state.json) <= mtime(report)` → reuse existing
       report. Log `"Reusing existing planning report from {date}"`.
     - `mtime(execution-state.json) > mtime(report)` → regenerate. Log
       `"Regenerating planning report (execution state modified)"`.

### 3.5 Console Summary

Log the final summary to console:

```
=== Epic Planning Complete ===

EPIC-{epicId}: {overall_status}

  Stories: {stories_planned}/{stories_total} planned
  Ready:   {stories_ready}
  Not Ready: {stories_not_ready}
  Pending: {stories_pending}

  Report: ai/epics/epic-{epicId}/reports/epic-planning-report-{epicId}.md
  State:  ai/epics/epic-{epicId}/execution-state.json
```

```text
<!-- TELEMETRY: phase.end -->
Bash command: $CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end x-orchestrate-epic Phase-3-Consolidation ok
```

```text
Skill(skill: "x-internal-verify-phase-gates", model: "haiku", args: "--mode final --skill x-orchestrate-epic --phase Phase-3-Report --expected-artifacts ai/epics/epic-{epicId}/reports/epic-planning-report-{epicId}.md,ai/epics/epic-{epicId}/execution-state.json")
TaskUpdate(id: phase3TaskId, status: "completed")
```

## Step P5 — Push Epic Branch to Origin (optional, EPIC-0049)

```text
<!-- TELEMETRY: phase.start -->
Bash command: $CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start x-orchestrate-epic Phase-P5-Push
```

If `--dry-run` is set, log `"dry-run, skipping push"` and skip.

Delegate the push of the canonical `epic/<XXXX>` branch to origin:

```text
Skill(skill: "x-push-branch", args: "--branch epic/<XXXX>")
```

On push failure (remote rejection, no connectivity), log a WARNING and
continue — wave commits are preserved locally. Do NOT abort.

```text
<!-- TELEMETRY: phase.end -->
Bash command: $CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end x-orchestrate-epic Phase-P5-Push ok
```

`>>> Phase 3/3 completed. Epic planning complete.`

---

## Checkpoint / Resume Protocol

### Checkpoint Mechanism

`execution-state.json` in `{epicDir}/` serves as the checkpoint file. It is
updated atomically after each story completes planning.

### Resume Behavior (`--resume`)

When `--resume` is set:

1. Load `execution-state.json` from `{epicDir}/`.
2. Apply reclassification (see Phase 0.6).
3. Stories with `planningStatus == "READY"` are **skipped** (artifacts
   already generated).
4. Stories with `planningStatus == "IN_PROGRESS"` are **reset to PENDING**
   (interrupted work).
5. Stories with `planningStatus == "NOT_READY"` are **reset to PENDING**
   (re-attempt).
6. Stories with `planningStatus == "PENDING"` proceed normally.
7. Enter Phase 2 plan loop with the filtered story set.

### Idempotency

- Running `/x-orchestrate-epic XXXX` twice without `--resume` regenerates
  all artifacts (full run).
- Running `/x-orchestrate-epic XXXX --resume` after a partial run only
  plans stories not yet `READY`.
- `/x-plan-story` itself has a staleness check (RULE-002): if source files
  have not changed, it reuses existing artifacts.

---

## Flat File Naming Convention (RULE-004)

All output files follow the flat naming convention under `{epicDir}/plans/`.
Per-task artifacts have two schemas dispatched by `/x-plan-story` based on
`planningSchemaVersion` (see `x-plan-story/SKILL.md` §Output Contract):

- **v1 (`planningSchemaVersion: "1.0"` or absent)** — single task-plan file
  per task: `task-plan-TASK-NNN-story-XXXX-YYYY.md`.
- **v2 (`planningSchemaVersion: "2.0"`)** — split layout with one task file
  + one plan-task file + one task implementation map per story:
  `task-TASK-XXXX-YYYY-NNN.md`, `plan-task-TASK-XXXX-YYYY-NNN.md`,
  `task-implementation-map-STORY-XXXX-YYYY.md`.

```
{epicDir}/
  EPIC-XXXX.md
  IMPLEMENTATION-MAP.md
  story-XXXX-0001.md
  story-XXXX-0002.md
  execution-state.json
  plans/
    tasks-story-XXXX-0001.md
    tasks-story-XXXX-0002.md
    planning-report-story-XXXX-0001.md
    planning-report-story-XXXX-0002.md
    dor-story-XXXX-0001.md
    dor-story-XXXX-0002.md
    # v1 task-plan layout (planningSchemaVersion: "1.0")
    task-plan-TASK-001-story-XXXX-0001.md
    task-plan-TASK-002-story-XXXX-0001.md
    # v2 split layout (planningSchemaVersion: "2.0")
    plan-story-XXXX-0001.md
    task-TASK-XXXX-0001-001.md
    plan-task-TASK-XXXX-0001-001.md
    task-implementation-map-STORY-XXXX-0001.md
  reports/
    epic-planning-report-XXXX.md
```

The orchestrator commits whichever per-task pattern the subagent emits; v1 and
v2 are mutually exclusive within a single story but MAY coexist across stories
during a migration window.

---

## Integration with `/x-implement-epic`

The `execution-state.json` produced by `/x-orchestrate-epic` uses the same
top-level schema as `/x-implement-epic` (including `baseBranch`, `stories`
array, and `totalPhases`). Planning-specific fields are stored under
`storyEntries` alongside implementation fields:

| Field | Used By | Values |
|-------|---------|--------|
| `status` | x-implement-epic | `PENDING`, `IN_PROGRESS`, `SUCCESS`, `FAILED`, `PARTIAL`, `BLOCKED` |
| `planningStatus` | x-orchestrate-epic | `PENDING`, `IN_PROGRESS`, `READY`, `NOT_READY` |

Both fields coexist on the same story entry in `storyEntries`.
`/x-implement-epic` can use `planningStatus == "READY"` as a pre-condition
gate before dispatching a story for implementation. Neither skill
overwrites fields owned by the other.

---

## Skills Dependency Graph

```
/x-orchestrate-epic (this skill)
  |
  |-- Step P1: x-manage-worktrees detect-context          (advisory)
  |-- Step P2: x-internal-ensure-epic-branch              (canonical branch)
  |-- reads: IMPLEMENTATION-MAP.md (phase/dependency graph)
  |-- reads: EPIC-XXXX.md (epic context)
  |-- reads: story-XXXX-*.md (story files)
  |-- reads/writes: execution-state.json (checkpoint)
  |
  +-- per story (subagent): /x-plan-story --no-commit
  |     |
  |     +-- 5 parallel subagents: Architect, QA, Security, Tech Lead, PO
  |
  |-- Step P4 (per wave): x-commit-planning               (batch commit per wave)
  +-- Step P5 (end):      x-push-branch                   (push epic branch)
```

---

## Constraints

- **Memory management.** Limit parallel subagent dispatch to 3 stories per
  batch to avoid OOM (each `/x-plan-story` spawns 5 internal subagents).
- **Checkpoint atomicity.** Always write `execution-state.json` after each
  story completes; never batch-update.
- **Staleness check (RULE-002).** `/x-plan-story` handles its own staleness
  check; `/x-orchestrate-epic` only checks for the planning report.
- **Content language (RULE-006).** User-facing planning report content in
  pt-BR; technical fields and log messages in English.
