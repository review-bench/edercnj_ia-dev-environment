---
name: x-story-implement
model: sonnet
description: "Thin orchestrator (~320 lines — story-0049-0019 refactor) that drives a story end-to-end via 4 delegated phases: Phase 0 (args via x-internal-args-normalize + context via x-internal-story-load-context + resume via x-internal-story-resume), Phase 1 (parallel planning via x-internal-story-build-plan), Phase 2 (task execution loop via x-task-implement per task, then final story PR via x-pr-create), Phase 3 (verify via x-internal-story-verify + report via x-internal-story-report + optional worktree cleanup via x-git-worktree). New EPIC-0049 flags --target-branch / --auto-merge / --epic-id propagate OO-style to x-task-implement and x-pr-create. Backward compatible: absent flags preserve legacy EPIC-0048 behavior (target=develop, auto-merge=none)."
user-invocable: true
allowed-tools: Read, Write, Edit, Bash, Grep, Glob, Skill, Agent, TaskCreate, TaskUpdate, AskUserQuestion
argument-hint: "[STORY-ID] [--target-branch <branch>] [--auto-merge <merge|squash|rebase|none>] [--epic-id <XXXX>] [--auto-approve-pr] [--task TASK-ID] [--resume] [--skip-verification] [--skip-smoke] [--skip-review] [--full-lifecycle] [--worktree] [--non-interactive] [--no-auto-remediation] [--no-ci-watch]"
context-budget: medium
requires-capabilities: []
---

## Output Policy & Context Management

English ONLY; technical, direct, concise (no conversational fillers). Record only compact envelopes from `x-task-implement` (status/taskId/commitSha/coverage*) — no full TDD logs. Reference plan/review files by path (+score for reviews); never re-load content. Read story file once in Phase 0 via `x-internal-story-load-context`. Delegate every state mutation to `x-internal-status-update`.

# Skill: Story Implementation (Thin Orchestrator — ADR-0012 + EPIC-0049)

Orchestrate story end-to-end via delegation. Inline: argv parse (delegate), load context (delegate), drive task loop (`x-task-implement`), close story-level PR. Zero direct git/gh/mvn calls.

**EPIC-0049 flag propagation (OO-style — RULE-009):** `--target-branch <branch>` (default `develop`) → `x-task-implement` + `x-pr-create`; `--auto-merge <strategy>` (default `none`) → `x-pr-create`; `--epic-id <ID>` (auto-derived from storyId) → `x-pr-create` (adds `epic-XXXX` label). All flags absent = EPIC-0048 behavior (backward compat — RULE-008).

## Triggers

- `/x-story-implement story-XXXX-YYYY` — full lifecycle (legacy default: target=develop, no auto-merge)
- `/x-story-implement story-XXXX-YYYY --target-branch epic/0049 --auto-merge merge --epic-id 0049` — orchestrator-propagated run targeting an epic branch
- `/x-story-implement story-XXXX-YYYY --task TASK-NNN` — execute only a specific task (resume / targeted)
- `/x-story-implement story-XXXX-YYYY --resume` — continue from `execution-state.json` via `x-internal-story-resume`
- `/x-story-implement story-XXXX-YYYY --auto-approve-pr` — parent-branch mode (RULE-004)
- `/x-story-implement story-XXXX-YYYY --worktree` — standalone opt-in worktree (ADR-0004 Mode 2)
- `/x-story-implement story-XXXX-YYYY --interactive` — opt-in to gate menus (PROCEED/FIX-PR/ABORT)

## Review Policy — `MANDATORY — NON-NEGOTIABLE`: Specialist (`x-review`) + Tech-Lead (`x-review-pr`) reviews in Step 3.2 MUST execute unless `--skip-verification` / `--skip-review`; silent omission is a `PROTOCOL_VIOLATION` and subagents MUST abort with `"REVIEW_SKIPPED_WITHOUT_FLAG"`.

## Parameters

| Flag | Type | Default | Description |
|------|------|---------|-------------|
| `STORY-ID` | String | — | Positional, required. Pattern `story-XXXX-YYYY`. |
| `--target-branch` | String | `develop` | Base branch for task + story PRs. Propagated to `x-task-implement` + `x-pr-create`. |
| `--auto-merge` | Enum | `none` | `merge\|squash\|rebase\|none`. Propagated to `x-pr-create`. Requires `--target-branch` when not `none`. |
| `--epic-id` | String(4) | auto | Auto-derived from story prefix. Propagated to `x-pr-create` (adds `epic-XXXX` label). |
| `--auto-approve-pr` | Boolean | `false` | Parent-branch mode (RULE-004): task PRs target `feat/story-...`. |
| `--task` | String | — | Execute only `TASK-XXXX-YYYY-NNN`. |
| `--resume` | Boolean | `false` | Delegates resume-point detection to `x-internal-story-resume`. |
| `--skip-verification` | Boolean | `false` | **Recovery-only.** Skips Phase 3; flagged outside `## Recovery` blocks. |
| `--skip-smoke`, `--skip-review` | Boolean | `false` | Bypass smoke gate / specialist + TL reviews (the only supported per-review bypass path). |
| `--full-lifecycle`, `--worktree` | Boolean | `false` | Full execution / standalone worktree mode. |
| `--interactive` | Boolean | `false` | Opt-in to 3-option gate menus (PROCEED/FIX-PR/ABORT). Default: non-interactive (Rule 20, EPIC-0061). |
| `--non-interactive` | Boolean | **DEPRECATED** | Was CI opt-in; now equals default. Emits WARN. Removed in 2 releases. |
| `--no-auto-remediation`, `--no-ci-watch` | Boolean | `false` | Skip Step 3.5 remediation / Rule 21 CI-watch. |

**Deprecated (no-op, warn-once):** `--manual-contract-approval`, `--manual-task-approval` (both since EPIC-0043).

## Output Contract

| Field | Type | Description |
|-------|------|-------------|
| `storyId`, `epicId` | String | Story + auto-derived epic ID |
| `status` | Enum | `SUCCESS \| FAILED \| PARTIAL` |
| `targetBranch`, `autoMergeStrategy` | String, Enum | Resolved base branch + merge strategy |
| `tasksExecuted` | List<{id, status, commitSha, prNumber}> | One entry per task dispatched |
| `prNumber` / `prUrl` | Integer\|null / String\|null | Story-level PR; null without `--auto-approve-pr` |
| `verifyPassed` | Boolean | Phase 3 verify-gate `passed` |
| `reportPath` | String | Final report from `x-internal-story-report` |
| `coverageLine` / `coverageBranch` | Number\|null | Filtered coverage |

## Error Codes

| Exit | Code | Condition |
|------|------|-----------|
| 1 | `ARGS_INVALID` | `x-internal-args-normalize` exit 1 |
| 2 | `STORY_NOT_LOADABLE` | `x-internal-story-load-context` non-zero |
| 3 | `PLAN_FAILED` | `x-internal-story-build-plan` non-zero |
| 4 | `TASK_FAILED` | One or more tasks returned `FAILED` |
| 5 | `VERIFY_FAILED` | `x-internal-story-verify` `passed=false` |
| 6 | `PR_CREATE_FAILED` | `x-pr-create` non-zero on story-level PR |

## Workflow Overview

```
Phase 0 : Args, Context & Resume  (x-internal-args-normalize + load-context + resume — ~80 lines)
Phase 1 : Plan                    (x-internal-story-build-plan — ~50 lines)
Phase 2 : Task Execution Loop     (x-task-implement per task + x-pr-create — ~120 lines)
Phase 3 : Verify, Report, Cleanup (x-internal-story-verify + x-internal-story-report + x-git-worktree remove — ~70 lines)
```

## CRITICAL EXECUTION RULE

**4 phases (0–3). ALL phases through Phase 2 are mandatory. NEVER stop before Phase 2 completes.** Print `>>> Phase N completed. Proceeding to Phase N+1 (...)...` between phases; `>>> Phase 3 completed. Lifecycle complete.` at end.

## Phase 0 — Args, Context & Resume

<!-- TELEMETRY: phase.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start x-story-implement Phase-0-Prepare`

Skill(skill: "x-internal-phase-gate", model: "haiku", args: "--mode pre --skill x-story-implement --phase Phase-0-Context")

Open phase tracker (close with `TaskUpdate(id: phase0TaskId, status: "completed")` after §0.4):

    TaskCreate(subject: "{STORY_ID} › Phase 0 - Context", activeForm: "Loading story context and resume state")

### 0.1 Args normalization (Rule 13 Pattern 1)

    Skill(skill: "x-internal-args-normalize", args: "--schema @references/args-schema.json --argv \"{raw argv}\"")

Consume `{parsed, warnings, errors}`: exit `ARGS_INVALID` on errors; print warnings once; extract `storyId`, `epicId` (auto-derived from storyId prefix), `targetBranch`, `autoMerge`, and all flags (skipReview/skipVerification/skipSmoke/autoApprovePr/nonInteractive/worktree/task/resume/noAutoRemediation/noCiWatch/fullLifecycle) for phase propagation.

### 0.1a Persist interactiveMode (EPIC-0068)

Write the resolved interactive mode to `execution-state.json` so the Stop hook `enforce-continuous-flow.sh` can determine whether to emit nudges:

    Skill(skill: "x-internal-status-update", args: "--file ai/epics/epic-XXXX/execution-state.json --type story --id <STORY-ID> --field interactiveMode --value <interactive|non-interactive>")

Value is `"interactive"` when `--interactive` flag was passed or `CLAUDE_LEGACY_INTERACTIVE=1`; otherwise `"non-interactive"` (Rule 20 default, EPIC-0061).

### 0.2 Load story context

    Skill(skill: "x-internal-story-load-context", args: "--story-id <STORY-ID> --epic-id <EPIC-ID>")

Sub-skill reads story, validates predecessors, runs 7-artifact staleness pre-check (arch/impl/test/tasks/security/compliance/per-task plans), classifies scope (SIMPLE/STANDARD/COMPLEX), detects planningMode (PRE_PLANNED/HYBRID/INLINE). Envelope: `{storyPath, tasksCount, scope, planningMode, predecessorsComplete, artifactFreshness[]}`. Non-zero → `STORY_NOT_LOADABLE`.

### 0.3 Worktree-first branch decision (Rule 14 + ADR-0004)

`Skill(skill: "x-git-worktree", args: "detect-context")` → apply 3-way decision (REUSE/CREATE/LEGACY); set `STORY_OWNS_WORKTREE` in-memory. Full table in `references/full-protocol.md` §1.

### 0.4 Resume (conditional on `--resume`)

    Skill(skill: "x-internal-story-resume", args: "--story-id <STORY-ID> --epic-id <EPIC-ID>")

Envelope: `{resumePoint, tasksCompleted[], tasksPending[], lastCommitSha, staleWarnings[]}`. Use `tasksPending` as Phase 2 iteration filter; print staleWarnings once.

Skill(skill: "x-internal-phase-gate", model: "haiku", args: "--mode post --skill x-story-implement --phase Phase-0-Context")

TaskUpdate(id: phase0TaskId, status: "completed")

<!-- TELEMETRY: phase.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end x-story-implement Phase-0-Prepare ok`

>>> Phase 0 completed. Proceeding to Phase 1 (Plan)...

<!-- phase-no-gate: API-First is conditional — only activates for stories with contract interfaces; gates live inside the conditional block -->
## Phase 0.5 — API-First Contract Generation (Conditional)

Activates ONLY when `has_contract_interfaces=True` (story declares interfaces of type `rest`, `grpc`, `event-consumer`, `event-producer`, or `websocket`). Open phase tracker when active:

    TaskCreate(subject: "{STORY_ID} › Phase 0 - API Contract", activeForm: "Generating API-first contracts")

Generates draft OpenAPI 3.1 / Protobuf 3 / AsyncAPI 2.6 in `contracts/{STORY_ID}-*.yaml|proto`, validates via `x-test-contract-lint`, and pauses at **Step 0.5.4** via `AskUserQuestion` (EPIC-0043 gate — replaces legacy `CONTRACT PENDING APPROVAL` text). Full interface-to-format mapping table and generator details live in `references/full-protocol.md` §2.

    TaskUpdate(id: phase05TaskId, status: "completed")

## Phase 1 — Plan

<!-- TELEMETRY: phase.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start x-story-implement Phase-1-Plan`

Skill(skill: "x-internal-phase-gate", model: "haiku", args: "--mode pre --skill x-story-implement --phase Phase-1-Plan")

Open phase tracker (close with `TaskUpdate(id: phase1TaskId, status: "completed")` after parallelism gate):

    TaskCreate(subject: "{STORY_ID} › Phase 1 - Plan", activeForm: "Orchestrating parallel planning wave")

**Batch A — emit 6 planner task trackers in ONE message (SIMPLE scope skips security+compliance).** One TaskCreate per planner (Arch plan / Impl plan / Test plan / Task breakdown / Security assessment / Compliance assessment). Store IDs: `plannerTasks = {arch, impl, test, tasks, security, compliance}`.

    TaskCreate(subject: "{STORY_ID} › Phase 1 › Arch plan", activeForm: "Running arch plan")
    [+5 siblings per above naming] — see `references/full-protocol.md` §Phase-1-Planning-Wave for exhaustive list.

**Batch B — after `x-internal-story-build-plan` returns, emit 6 `TaskUpdate(id: plannerTasks.X, status: "completed")` in ONE message (arch/impl/test/tasks/security/compliance).**

    TaskUpdate(id: plannerTasks.arch, status: "completed")
    TaskUpdate(id: plannerTasks.impl, status: "completed")
    TaskUpdate(id: plannerTasks.test, status: "completed")
    TaskUpdate(id: plannerTasks.tasks, status: "completed")
    TaskUpdate(id: plannerTasks.security, status: "completed")
    TaskUpdate(id: plannerTasks.compliance, status: "completed")

**Skipped entirely when `planningMode=PRE_PLANNED`** (all 7 artifacts fresh — logged `"[phase-1] skipped — PRE_PLANNED"` and proceeding to Phase 2).

Otherwise invoke:

    Skill(skill: "x-internal-story-build-plan", args: "--story-id <STORY-ID> --epic-id <EPIC-ID> --scope <SCOPE> [--skip-review]")

The sub-skill orchestrates parallel story-planning (Phase 1 carve-out): invokes `x-arch-plan` (Step 1A) then dispatches up to 5 sibling Agent subagents in ONE assistant message for implementation plan, test plan, task breakdown, security assessment, and compliance assessment (Steps 1B-1F). Scope-aware: SIMPLE skips 1E/1F.

Consume the envelope `{artifacts: [{name, path, status}], planningMode, scopeResolved}`. On non-zero exit → exit with code `PLAN_FAILED`. Persist the envelope in orchestrator memory — never re-read the artifact files.

### Parallelism gate (RULE-004 hotspots)

Invoke `Skill(skill: "x-parallel-eval", args: "--scope=story --story=<STORY-ID>")` (EPIC-0041 / story-0041-0006). On detected hard/regen collision, degrade the affected wave to serial and record `ExecutionState.parallelismDowngrades`. Full matrix in `references/full-protocol.md` §3.

Wave gate — all planning artifacts produced:

    Skill(skill: "x-internal-phase-gate", model: "haiku", args: "--mode wave --skill x-story-implement --phase Phase-1-Plan --expected-tasks {planner-task-ids} --expected-artifacts {arch-path},{impl-path},{test-path},{tasks-path},{security-path},{compliance-path}")

Skill(skill: "x-internal-phase-gate", model: "haiku", args: "--mode post --skill x-story-implement --phase Phase-1-Plan")

TaskUpdate(id: phase1TaskId, status: "completed")

<!-- TELEMETRY: phase.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end x-story-implement Phase-1-Plan ok`

>>> Phase 1 completed. Proceeding to Phase 2 (Task Execution Loop)...

## Phase 2 — Task Execution Loop (RULE-001)

<!-- TELEMETRY: phase.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start x-story-implement Phase-2-Implement`

Skill(skill: "x-internal-phase-gate", model: "haiku", args: "--mode pre --skill x-story-implement --phase Phase-2-Execute")

Open phase tracker (close with `TaskUpdate(id: phase2TaskId, status: "completed")` after story-level PR):

    TaskCreate(subject: "{STORY_ID} › Phase 2 - Execute", activeForm: "Running task execution loop")

Read tasks from `ai/epics/epic-XXXX/plans/tasks-story-XXXX-YYYY.md` (Section 8 fallback when absent). Apply the `tasksPending` filter from Phase 0.4 when resuming. For each `TASK-XXXX-YYYY-NNN` not in `DONE`/`BLOCKED`:

**Per-task tracking (sequential, chained via `addBlockedBy`):**

    currentTaskId = TaskCreate(subject: "{STORY_ID} › Phase 2 › TASK-XXXX-YYYY-NNN", activeForm: "Executing TASK-XXXX-YYYY-NNN")
    [if previous task exists]: TaskUpdate(id: previousTaskId, addBlockedBy: [currentTaskId])

    ... dispatch x-task-implement + x-pr-create ...

    TaskUpdate(id: currentTaskId, status: "completed")

### 2.1 Per-task dispatch
1. Check deps against `execution-state.json`; unresolved → `BLOCKED` via `x-internal-status-update`, skip.
2. **Dispatch TDD:** `Skill(skill: "x-task-implement", model: "sonnet", args: "<TASK-ID> --orchestrated --target-branch <targetBranch> [--auto-merge <strategy>] [--epic-id <EPIC-ID>] [--auto-approve-pr] [--non-interactive]")` → RED/GREEN/REFACTOR + atomic commit + push `feat/task-XXXX-YYYY-NNN-desc`. Returns `{status, taskId, commitSha, branchName, coverageLine, coverageBranch}`.
3. **CI-watch (Rule 21 + Rule 45):** unless `--no-ci-watch`. **MANDATORY TOOL CALL — NON-NEGOTIABLE (Rule 24):** `Skill(skill: "x-pr-watch-ci", args: "--branch <branchName>")` — 8 exit codes. Persists `.claude/state/pr-watch-{PR}.json`; absence on a merged PR fails Camada 3 audit.
4. **PR creation (RULE-009 OO propagation):** `Skill(skill: "x-pr-create", model: "haiku", args: "<TASK-ID> --target-branch <targetBranch> --auto-merge <strategy> --epic-id <EPIC-ID> [--auto-approve-pr]")`. With `--auto-approve-pr`, task-PR target becomes parent story branch. Consume `{prUrl, prNumber, prMergeStatus}`.
5. **Status:** `Skill(skill: "x-internal-status-update", args: "--file ai/epics/epic-XXXX/execution-state.json --type task --id <TASK-ID> --field status --value <STATUS>")`.

### 2.2 Fail-fast + story-level PR
On `status=FAILED`, mark dependants `BLOCKED` and exit Phase 2 with `TASK_FAILED` (unless `--task` — single-task mode). When `--auto-approve-pr` is set, after all tasks succeed push parent branch and create story-level PR:

    Skill(skill: "x-pr-create", model: "haiku", args: "--story-id <STORY-ID> --head feat/story-<STORY-ID> --target-branch <targetBranch> --auto-merge <strategy> --epic-id <EPIC-ID>")

Without `--auto-approve-pr`, task PRs are the unit of delivery — no story PR. Non-zero exit → `PR_CREATE_FAILED`. Fallback G1-G7 in `references/full-protocol.md` §4.

Skill(skill: "x-internal-phase-gate", model: "haiku", args: "--mode post --skill x-story-implement --phase Phase-2-Execute")

TaskUpdate(id: phase2TaskId, status: "completed")

<!-- TELEMETRY: phase.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end x-story-implement Phase-2-Implement ok`

>>> Phase 2 completed. All tasks executed. Proceeding to Phase 3 (Verify & Report)...

## Phase 3 — Verify, Report & Cleanup

<!-- TELEMETRY: phase.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start x-story-implement Phase-3-Verify`

Skill(skill: "x-internal-phase-gate", model: "haiku", args: "--mode pre --skill x-story-implement --phase Phase-3-Verify")

Open phase tracker (close with `TaskUpdate(id: phase3TaskId, status: "completed")` at FINAL gate):

    TaskCreate(subject: "{STORY_ID} › Phase 3 - Verify", activeForm: "Running verify gate and reviews")

**Sub-task trackers (open in Batch A — one per sub-step 3.Q–3.5 — before `Skip verification`):** emit 13 `TaskCreate` in ONE message (one per Quality-regression / Quality-perf / Quality-mutation / Quality-contract / Quality-dep-policy / Doc generate / Doc validate / Verify gate / Specialist reviews / Tech lead review / Report / Status finalize / Worktree cleanup). Store IDs: `p3Tasks = {qualityRegression, qualityPerf, qualityMutation, qualityContract, qualityDepPolicy, docGenerate, docValidate, verify, specialist, techLead, report, status, cleanup}`.

    TaskCreate(subject: "{STORY_ID} › Phase 3 › Quality regression", activeForm: "Running x-test-regression-shell")
    TaskCreate(subject: "{STORY_ID} › Phase 3 › Quality perf", activeForm: "Running x-test-performance")
    TaskCreate(subject: "{STORY_ID} › Phase 3 › Quality mutation", activeForm: "Running x-test-mutation")
    TaskCreate(subject: "{STORY_ID} › Phase 3 › Quality contract", activeForm: "Running x-test-contract")
    TaskCreate(subject: "{STORY_ID} › Phase 3 › Quality dep-policy", activeForm: "Running x-dep-policy-validate")
    TaskCreate(subject: "{STORY_ID} › Phase 3 › Doc generate", activeForm: "Running x-doc-generate")
    TaskCreate(subject: "{STORY_ID} › Phase 3 › Doc validate", activeForm: "Running x-doc-validate")
    TaskCreate(subject: "{STORY_ID} › Phase 3 › Verify gate", activeForm: "Running story verify gate")
    TaskCreate(subject: "{STORY_ID} › Phase 3 › Specialist reviews", activeForm: "Running specialist reviews")
    TaskCreate(subject: "{STORY_ID} › Phase 3 › Tech lead review", activeForm: "Running tech lead review")
    TaskCreate(subject: "{STORY_ID} › Phase 3 › Report", activeForm: "Generating story completion report")
    TaskCreate(subject: "{STORY_ID} › Phase 3 › Status finalize", activeForm: "Finalizing story status")
    TaskCreate(subject: "{STORY_ID} › Phase 3 › Worktree cleanup", activeForm: "Cleaning up worktree")

**Skipped only under `--skip-verification`** (recovery-only — see Rule 22). Every `Skill(...)` below is a **MANDATORY TOOL CALL** (Rule 24); inlining is a violation and the CI audit fails merges lacking evidence artifacts. Full per-step details (sub-skill envelopes, NO-GO cycle protocol, worktree-cleanup decision table) in `references/full-protocol.md` §5.

### 3.Q Quality Gates — **MANDATORY conditional** (EPIC-0072 + EPIC-0073 + EPIC-0074, Rule 24)
> Sequence: regression → perf → mutation → contract → dep-policy (D-R11 fast-fail). `QualityConfig`/`DependencyPolicyConfig` from project YAML. Disabled sub-gates emit WARN; no report produced. First non-zero exit cancels remaining gates.
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start x-story-implement Phase-3-Quality-Regression`
    Skill(skill: "x-test-regression-shell", model: "sonnet", args: "--story-id <STORY-ID> --report ai/epics/epic-XXXX/reports/regression-report-STORY-ID.md")  [conditional: flag.quality_regression_enabled]
    TaskUpdate(id: p3Tasks.qualityRegression, status: "completed")
Exit non-zero → `REGRESSION_DETECTED` (19); **D-R11: skip 3.Q.1, 3.Q.2, and 3.Q.3**.
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end x-story-implement Phase-3-Quality-Regression ok`
<!-- TELEMETRY: phase.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start x-story-implement Phase-3-Quality-Perf`
    Skill(skill: "x-test-performance", model: "sonnet", args: "--story-id <STORY-ID> --report ai/epics/epic-XXXX/reports/perf-report-STORY-ID.md")  [conditional: flag.quality_performance_enabled]
    TaskUpdate(id: p3Tasks.qualityPerf, status: "completed")
Exit non-zero → `PERF_REGRESSION_DETECTED` (14); **D-R11: skip 3.Q.2 and 3.Q.3**.
<!-- TELEMETRY: phase.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end x-story-implement Phase-3-Quality-Perf ok`
<!-- TELEMETRY: phase.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start x-story-implement Phase-3-Quality-Mutation`
    Skill(skill: "x-test-mutation", model: "sonnet", args: "--story-id <STORY-ID> --report ai/epics/epic-XXXX/reports/mutation-report-STORY-ID.md")  [conditional: flag.quality_mutation_enabled]
    TaskUpdate(id: p3Tasks.qualityMutation, status: "completed")
Exit non-zero → `MUTATION_SCORE_BELOW_THRESHOLD` (17); **D-R11: skip 3.Q.3**.
<!-- TELEMETRY: phase.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end x-story-implement Phase-3-Quality-Mutation ok`
<!-- TELEMETRY: phase.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start x-story-implement Phase-3-Quality-Contract`
    Skill(skill: "x-test-contract", model: "sonnet", args: "--story-id <STORY-ID> --report ai/epics/epic-XXXX/reports/contract-report-STORY-ID.md")  [conditional: flag.quality_contract_enabled]
    TaskUpdate(id: p3Tasks.qualityContract, status: "completed")
Exit non-zero → `CONTRACT_BREAKING_CHANGE` (18).
<!-- TELEMETRY: phase.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end x-story-implement Phase-3-Quality-Contract ok`
<!-- TELEMETRY: phase.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start x-story-implement Phase-3-Quality-DepPolicy`
    Skill(skill: "x-dep-policy-validate", model: "haiku", args: "--story-id <STORY-ID> --report ai/epics/epic-XXXX/reports/dep-policy-validation-report-STORY-ID.md")  [conditional: flag.dep_policy_enabled]
    TaskUpdate(id: p3Tasks.qualityDepPolicy, status: "completed")
Exit non-zero → `DEP_POLICY_BLOCK` (20); gate fails, merge blocked (Rule 32 §Enforcement).
<!-- TELEMETRY: phase.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end x-story-implement Phase-3-Quality-DepPolicy ok`

### 3.0 Documentation — `MANDATORY — NON-NEGOTIABLE` (Rule 31, EPIC-0071)

`--skip-doc` in `## Recovery`/`hotfix/*` only (Rule 27 Ex. 2). Retry/abort: `references/full-protocol.md` §5.

    Skill(skill: "x-doc-generate", model: "sonnet", args: "--story-id <STORY-ID>")  [required]
    TaskUpdate(id: p3Tasks.docGenerate, status: "completed")
    Skill(skill: "x-doc-validate", model: "sonnet", args: "--story-id <STORY-ID> --report-path ai/epics/epic-XXXX/reports/doc-validate-report-STORY-ID.md")  [required]
    TaskUpdate(id: p3Tasks.docValidate, status: "completed")

### 3.1 Verify gate — MANDATORY TOOL CALL

    Skill(skill: "x-internal-story-verify", args: "--story-id <STORY-ID> --epic-id <EPIC-ID> [--coverage-threshold-line 95] [--coverage-threshold-branch 90]")
    TaskUpdate(id: p3Tasks.verify, status: "completed")

Persists `ai/epics/epic-XXXX/reports/verify-envelope-STORY-ID.json`. On `passed=false` → `VERIFY_FAILED`.

### 3.2 Specialist + Tech-Lead reviews — `MANDATORY — NON-NEGOTIABLE` (unless `--skip-review`)
Both MUST execute in sequence; silent omission is `PROTOCOL_VIOLATION`. NO-GO cycle protocol in `references/full-protocol.md` §5.
    Skill(skill: "x-review", model: "sonnet", args: "<STORY-ID>")
    TaskUpdate(id: p3Tasks.specialist, status: "completed")
    Skill(skill: "x-review-pr", model: "sonnet", args: "<STORY-ID>")
    TaskUpdate(id: p3Tasks.techLead, status: "completed")
On Tech-Lead GO, optional `x-pr-fix` unless `--no-auto-remediation`.

### 3.3 Final report — MANDATORY TOOL CALL

    Skill(skill: "x-internal-story-report", args: "--story-id <STORY-ID> --epic-id <EPIC-ID> --output ai/epics/epic-XXXX/reports/story-completion-report-STORY-ID.md")
    TaskUpdate(id: p3Tasks.report, status: "completed")

### 3.4 Status finalize
Finalize atomically via `x-internal-status-update`: IMPLEMENTATION-MAP + Story `**Status:**` → `Concluída`, Jira → `Done`, state.json → `COMPLETE`.

    TaskUpdate(id: p3Tasks.status, status: "completed")

### 3.5 Worktree cleanup (standalone Mode 2 only)
Gated by `STORY_OWNS_WORKTREE`: `true`+passed → `Skill(skill: "x-git-worktree", args: "remove --id story-XXXX-YYYY")` + re-sync develop; `true`+failed → preserve (Rule 14 §4); `false` → skip.

    TaskUpdate(id: p3Tasks.cleanup, status: "completed")

Skill(skill: "x-internal-phase-gate", model: "haiku", args: "--mode final --skill x-story-implement --phase Phase-3-Verify --expected-artifacts ai/epics/epic-XXXX/reports/verify-envelope-STORY-ID.json,ai/epics/epic-XXXX/plans/review-story-STORY-ID.md,ai/epics/epic-XXXX/plans/techlead-review-story-STORY-ID.md,ai/epics/epic-XXXX/reports/story-completion-report-STORY-ID.md,ai/epics/epic-XXXX/reports/doc-validate-report-STORY-ID.md")

TaskUpdate(id: phase3TaskId, status: "completed")

<!-- TELEMETRY: phase.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end x-story-implement Phase-3-Verify ok`

>>> Phase 3 completed. Lifecycle complete.

## Error Envelope

| Scenario | Action |
|----------|--------|
| Args invalid | `x-internal-args-normalize` non-zero → `ARGS_INVALID` |
| Story file not found / dependency incomplete | `x-internal-story-load-context` non-zero → `STORY_NOT_LOADABLE` |
| Task dependency unresolved | Mark `BLOCKED` via `x-internal-status-update`; continue |
| `x-task-implement` fails | Mark `FAILED`; block propagation; `TASK_FAILED` (unless `--task`) |
| `x-doc-validate` fails after retry | `DOC_VALIDATION_FAILED` — abort Phase 3; operator must run `/x-doc-generate` + `--resume` |
| Coverage / AC / consistency failure | `x-internal-story-verify` `passed=false` → `VERIFY_FAILED` |
| `x-pr-create` fails | Task → `FAILED`; story-level → `PR_CREATE_FAILED` |
| `x-test-performance` regression | `PERF_REGRESSION_DETECTED` (exit 14) — FIX-PR: investigate benchmark delta |
| `x-test-mutation` below threshold | `MUTATION_SCORE_BELOW_THRESHOLD` (exit 17) — FIX-PR: kill surviving mutants |
| `x-test-contract` breaking change | `CONTRACT_BREAKING_CHANGE` (exit 18) — FIX-PR: revert or bump MAJOR |
| `x-dep-policy-validate` blocks | `DEP_POLICY_BLOCK` (exit 20) — FIX-PR: remediate denied CVE / license / version violation (Rule 32) |
| `x-pr-fix` compile regression | ABORT Step 3.2 with `PR_FIX_COMPILE_REGRESSION` |
| Template missing (RULE-012) | WARN; `x-internal-report-write` degrades to inline format |
| Corrupted `execution-state.json` on resume | `x-internal-story-resume` reinitializes |
| Review NO-GO > 2 cycles | Keep `verifyPassed=true`; flag human-gate WARNING |
| Sub-skill transient error | Retry once with 2s backoff; persistent → propagate |

Full retry/backoff schedule + `SubagentResult` error shape live in `references/full-protocol.md` §6.

## Recovery

Resuming after an aborted lifecycle may legitimately skip already-completed steps. The canonical signal is `CLAUDE_RECOVERY_MODE=1` — set automatically by `x-internal-story-resume` when `staleWarnings != []`. The PreToolUse hook `enforce-no-bypass-flags.sh` (EPIC-0059, Rule 45) then permits the flags below with a WARNING; skips remain visible in telemetry as `phase.end status=skipped`. **Operator-controlled sessions only — never in automated calls.** `CLAUDE_RECOVERY_MODE=1` is the ONLY accepted bypass variable; `CLAUDE_SKIP_AUDIT=1` / `CLAUDE_NO_ENFORCE=1` do NOT bypass (RULE-059-07).

| Flag | Skips |
| :--- | :--- |
| `--skip-verification` | Phase 3 (`x-internal-story-verify`) |
| `--skip-review` | Step 3.2 (`x-review` + `x-review-pr`) |
| `--skip-quality` | Step 3.Q (`x-test-performance` + `x-test-mutation` + `x-test-contract` + `x-dep-policy-validate`) — **Recovery or hotfix/* only** |
| `--skip-doc` | Step 3.0 (`x-doc-generate` + `x-doc-validate`) — **Recovery or hotfix/* only (Rule 27 Exception 2)** |
| `--skip-smoke` | Smoke gate inside verify |
| `--no-ci-watch` | CI-watch step in Phase 2 |

## Backward Compatibility (RULE-008) + Idempotency (RULE-002)
All new EPIC-0049 flags absent → `targetBranch=develop`, `autoMerge=none`, `epicId` auto-derived — identical to EPIC-0048. `--auto-merge` without `--target-branch` → `ARGS_INVALID` (mutex). Idempotent: story load read-only, artifacts regen only on staleness, task dispatch short-circuits merged PRs, status mutations flock-protected, story PR re-run returns existing `{prUrl, prNumber}`. Full tables in `references/full-protocol.md` §7-8.

## Integration Notes
`x-internal-args-normalize` (0.1), `x-internal-story-load-context` (0.2), `x-internal-story-resume` (0.4 cond.), `x-internal-story-build-plan` (1), `x-task-implement` (2 per-task), `x-pr-create` (2 per-task+story), `x-pr-watch-ci` (2), `x-parallel-eval` (1), `x-review`/`x-review-pr`/`x-pr-fix` (3.2), `x-internal-story-verify` (3.1), `x-internal-story-report` (3.3), `x-internal-status-update` (all phases), `x-git-worktree` (0.3+3.5), `x-epic-implement` (caller).

## Full Protocol

Minimum viable orchestrator contract above. Complete Phase 0.3 worktree-mode decision table, Phase 1 parallelism matrix, Phase 2 fallback G1-G7 (no test plan / no formal tasks), Phase 3 verify-gate schemas + specialist-review prompt templates + TL-review NO-GO cycle protocol, resume workflow detail, retry/backoff/circuit-breaker schedule, and the `SubagentResult` error shape all live in [`references/full-protocol.md`](references/full-protocol.md) per ADR-0012.
