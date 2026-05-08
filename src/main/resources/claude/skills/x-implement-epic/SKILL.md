---
name: x-implement-epic
model: sonnet
description: "Drives an epic end-to-end via 6 phases: plan, branch, story loop, integrity gate, final PR."
user-invocable: true
allowed-tools: Read, Write, Glob, Skill, Agent, AskUserQuestion, TaskCreate, TaskUpdate
argument-hint: "[EPIC-ID] [--parallel] [--phase N] [--story story-XXXX-YYYY] [--resume] [--dry-run] [--skip-review] [--auto-merge-strategy merge|squash|rebase] [--strict-overlap] [--interactive] [--skip-pr-comments] [--revert-on-failure] [--skip-smoke]"
context-budget: medium
requires-capabilities: []
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

## Triggers

```
/x-implement-epic 0049                  — full run (sequential + auto-merge into epic/0049)
/x-implement-epic 0049 --parallel       — parallel story execution via worktrees
/x-implement-epic 0049 --resume         — continue from execution-state.json
/x-implement-epic 0049 --story story-0049-0007  — single story in isolation
/x-implement-epic 0049 --dry-run        — generate execution plan only, no dispatch
```

## Parameters

| Flag | Type | Default | Description |
|------|------|---------|-------------|
| `EPIC-ID` | String (4-digit) | — | Positional, required. |
| `--parallel` | Boolean | `false` | Opt-in parallel via worktrees. |
| `--phase N` | Integer | — | Execute only phase N stories. Mutually exclusive with `--story`. |
| `--story ID` | String | — | Execute a single story. Mutually exclusive with `--phase`. |
| `--resume` | Boolean | `false` | Continue from checkpoint. |
| `--dry-run` | Boolean | `false` | Generate execution plan; exit after Phase 1. |
| `--skip-review` | Boolean | `false` | Propagated to `x-implement-story` — skips specialist/TL reviews. |
| `--auto-merge-strategy` | Enum | `merge` | Story-PR auto-merge strategy: `merge\|squash\|rebase`. |
| `--interactive` | Boolean | `false` | Opt-in to 3-option menus (PROCEED/FIX-PR/ABORT) at each gate. Default: non-interactive. |
| `--skip-pr-comments` | Boolean | `false` | Skip Phase 4b post-gate PR-comment remediation pass. |
| `--revert-on-failure` | Boolean | `false` | On integrity-gate failure, revert last story merge instead of remediation agent. |
| `--skip-smoke` | Boolean | `false` | Bypass epic smoke gate (advisory; emergency only). |

## Output Contract

| Field | Description |
|-------|-------------|
| `epicId` | 4-digit zero-padded epic identifier |
| `epicBranch` | `epic/XXXX` |
| `phasesExecuted` | List of `{name, durationSec, status}` per phase |
| `storiesExecuted` | List of `{id, status, prNumber, prUrl}` per dispatched story |
| `finalPrUrl/Number` | Final PR `epic/XXXX → develop` |
| `integrityGatePassed` | Phase 4 gate `passed` value |
| `coverageLine/Branch` | Filtered coverage from integrity gate envelope |
| `reportsDir` | `ai/epics/epic-XXXX/reports/` |

**Delegation Map (RULE-005 — zero inline shell invocations):**

| Concern | Skill | Phase |
|---------|-------|-------|
| Args parsing | `x-internal-normalize-args` | 0 |
| DAG + execution plan | `x-internal-build-epic-plan` | 1 |
| `epic/<ID>` branch | `x-internal-ensure-epic-branch` | 2 |
| Per-story TDD + PR | `x-implement-story` | 3 |
| Integrity gate + report | `x-internal-verify-epic-integrity` + `x-internal-write-report` | 4 |
| Develop sync + final PR | `x-merge-branches` + `x-create-pr` | 5 |
| Status mutations | `x-internal-update-status` | all |
| Post-gate remediation | `x-fix-epic-pr` | 4b (optional) |

**Workflow:** Phase 0 (Args) → Phase 1 (Load & Plan) → Phase 2 (Branch Setup) → Phase 3 (Story Loop) → Phase 4 (Integrity Gate) → Phase 5 (Final PR).

## Phase 0 — Args

<!-- TELEMETRY: phase.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start x-implement-epic Phase-0-Args`

Skill(skill: "x-internal-verify-phase-gates", model: "haiku", args: "--mode pre --skill x-implement-epic --phase Phase-0-Args")

Open phase tracker (close with `TaskUpdate(id: phase0TaskId, status: "completed")` after args normalization):

    TaskCreate(subject: "EPIC-XXXX › Phase 0 - Args", activeForm: "Normalizing args")

Invoke args normalizer and resolve epicId, flags:

    Skill(skill: "x-internal-normalize-args", args: "--schema @references/args-schema.json --argv \"{raw argv}\"")

Persist interactiveMode to execution-state.json (EPIC-0068 — consumed by Stop hook `enforce-continuous-flow.sh`):

    Skill(skill: "x-internal-update-status", args: "--file ai/epics/epic-XXXX/execution-state.json --type epic --id <EPIC-ID> --field interactiveMode --value <interactive|non-interactive>")

Value: `"interactive"` when `--interactive` flag was passed; otherwise `"non-interactive"` (Rule 20 default).

Skill(skill: "x-internal-verify-phase-gates", model: "haiku", args: "--mode post --skill x-implement-epic --phase Phase-0-Args")

TaskUpdate(id: phase0TaskId, status: "completed")

<!-- TELEMETRY: phase.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end x-implement-epic Phase-0-Args ok`

## Phase 1 — Load and Plan

<!-- TELEMETRY: phase.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start x-implement-epic Phase-1-Plan`

Skill(skill: "x-internal-verify-phase-gates", model: "haiku", args: "--mode pre --skill x-implement-epic --phase Phase-1-Plan")

Open phase tracker (close with `TaskUpdate(id: phase1TaskId, status: "completed")` after plan build):

    TaskCreate(subject: "EPIC-XXXX › Phase 1 - Plan", activeForm: "Building epic execution plan")

Build DAG + execution plan:

    Skill(skill: "x-internal-build-epic-plan", args: "--epic-id <ID> --mode <sequential|parallel> --output ai/epics/epic-XXXX/reports/epic-execution-plan-XXXX.md [--strict-overlap]")

Consume `{phases, criticalPath, planPath}`. If `--dry-run=true` → print plan path and stop.

Skill(skill: "x-internal-verify-phase-gates", model: "haiku", args: "--mode post --skill x-implement-epic --phase Phase-1-Plan --expected-artifacts ai/epics/epic-XXXX/reports/epic-execution-plan-XXXX.md")

TaskUpdate(id: phase1TaskId, status: "completed")

<!-- TELEMETRY: phase.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end x-implement-epic Phase-1-Plan ok`

## Phase 2 — Branch Setup

<!-- TELEMETRY: phase.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start x-implement-epic Phase-2-Branch`

Open phase tracker (close after branch ensure):

    TaskCreate(subject: "EPIC-XXXX › Phase 2 - Branch", activeForm: "Ensuring epic branch exists")

Ensure `epic/<ID>` branch exists:

    Skill(skill: "x-internal-ensure-epic-branch", args: "--epic-id <ID> --base develop --push true")

On non-zero exit → `BRANCH_ENSURE_FAILED`.

    TaskUpdate(id: phase2TaskId, status: "completed")

<!-- TELEMETRY: phase.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end x-implement-epic Phase-2-Branch ok`

## Phase 3 — Story Loop

<!-- TELEMETRY: phase.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start x-implement-epic Phase-3-Execute`

Skill(skill: "x-internal-verify-phase-gates", model: "haiku", args: "--mode pre --skill x-implement-epic --phase Phase-3-Stories")

Open phase tracker (close with `TaskUpdate(id: phase3TaskId, status: "completed")` after all stories complete):

    TaskCreate(subject: "EPIC-XXXX › Phase 3 - Stories", activeForm: "Executing story loop")

> 🔒 **EXECUTION INTEGRITY (Rule 24):** Each `x-implement-story` call below is a **MANDATORY TOOL CALL**.

**Per-story tracking (sequential, chained via `addBlockedBy`):**

    currentStoryId = TaskCreate(subject: "EPIC-XXXX › Phase 3 › story-XXXX-YYYY", activeForm: "Implementing story-XXXX-YYYY")
    [if previous story exists]: TaskUpdate(id: previousStoryId, addBlockedBy: [currentStoryId])

    Skill(skill: "x-implement-story", model: "sonnet", args: "<STORY-ID> --target-branch <epicBranch> --auto-merge <strategy> [--skip-review] [--auto-approve-pr]")

    TaskUpdate(id: currentStoryId, status: "completed")

Phase gate: all stories in phase N must be `status=SUCCESS` AND `prMergeStatus=MERGED` before phase N+1. Failed story → block-propagation → `STORY_FAILED` (unless `--revert-on-failure`). Resume: Phase 1 envelope `resumeProjection` provides reclassified story statuses.

Skill(skill: "x-internal-verify-phase-gates", model: "haiku", args: "--mode post --skill x-implement-epic --phase Phase-3-Stories")

TaskUpdate(id: phase3TaskId, status: "completed")

<!-- TELEMETRY: phase.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end x-implement-epic Phase-3-Execute ok`

## Phase 4 — Integrity Gate

<!-- TELEMETRY: phase.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start x-implement-epic Phase-4-Gate`

Skill(skill: "x-internal-verify-phase-gates", model: "haiku", args: "--mode pre --skill x-implement-epic --phase Phase-4-Gate")

Open phase tracker (close with `TaskUpdate(id: phase4TaskId, status: "completed")` after report):

    TaskCreate(subject: "EPIC-XXXX › Phase 4 - Gate", activeForm: "Running integrity gate and report")

Run integrity gate:

    Skill(skill: "x-internal-verify-epic-integrity", args: "--epic-id <ID> --branch <epicBranch>")

On `passed=false`: remediation agent (or `--revert-on-failure` revert) + one retry → `INTEGRITY_GATE_FAILED`. Then write report:

    Skill(skill: "x-internal-write-report", args: "...")

Post-gate PR-comment remediation (unless `--skip-pr-comments`). **MANDATORY TOOL CALL — NON-NEGOTIABLE (Rule 24):** Invoke the `x-fix-epic-pr` skill via the Skill tool whenever the gate would otherwise leave actionable Copilot comments unaddressed:

    Skill(skill: "x-fix-epic-pr", model: "sonnet", args: "<EPIC-ID>")

Skill(skill: "x-internal-verify-phase-gates", model: "haiku", args: "--mode post --skill x-implement-epic --phase Phase-4-Gate")

TaskUpdate(id: phase4TaskId, status: "completed")

<!-- TELEMETRY: phase.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end x-implement-epic Phase-4-Gate ok`

## Phase 5 — Final PR

<!-- TELEMETRY: phase.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start x-implement-epic Phase-5-Final-PR`

Open phase tracker (close after PR creation):

    TaskCreate(subject: "EPIC-XXXX › Phase 5 - Final PR", activeForm: "Creating final PR epic/XXXX to develop")

Sync develop into epic branch:

    Skill(skill: "x-merge-branches", model: "haiku", args: "--source develop --target epic/<ID> --strategy merge")

On conflict → `FINAL_PR_CONFLICTS`. Then create final PR:

    Skill(skill: "x-create-pr", model: "haiku", args: "--epic-id <ID> --head epic/<ID> --target-branch develop --auto-merge none --label epic-integration")

Interactive menu (only when `--interactive`): PROCEED / FIX-PR / ABORT. Default is non-interactive (Rule 20, EPIC-0061).

**MANDATORY TOOL CALL — NON-NEGOTIABLE (Rule 24 + Rule 33):** Generate epic memory summary when `governance.ai-memory` capability is active:

    Skill(skill: "x-internal-summarize-epic", model: "haiku", args: "--epic-id <ID>")
    [conditional: flag.ai_memory_enabled]

Exit code handling: 0 = proceed; 7 `MANUAL_REFINEMENT_PRESENT` = proceed (human override preserved); any other non-zero → log warning, proceed (memory is best-effort; does not block PR creation).

    TaskUpdate(id: phase5TaskId, status: "completed")

<!-- TELEMETRY: phase.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end x-implement-epic Phase-5-Final-PR ok`

## Recovery

When resuming an epic that partially executed (e.g., some stories completed, CI watch failed mid-run), skip flags may be needed to bypass already-completed steps. Use `CLAUDE_RECOVERY_MODE=1`:

```bash
export CLAUDE_RECOVERY_MODE=1
/x-implement-epic EPIC-XXXX --resume --skip-review
```

### `CLAUDE_RECOVERY_MODE=1`

When this variable is set, the PreToolUse hook `enforce-no-bypass-flags.sh` (EPIC-0059, Rule 45) allows `--skip-review` and `--no-ci-watch` flags on this skill without blocking. A WARNING is emitted to stderr for audit trail. The variable is propagated automatically by `x-internal-resume-story` when `staleWarnings != []`.

**RULE-059-07:** `CLAUDE_RECOVERY_MODE=1` is the only accepted bypass variable. No other env var bypasses the enforcement hook.

### Permitted bypass flags (recovery only)

| Flag | Skips |
| :--- | :--- |
| `--skip-review` | Specialist + tech-lead reviews per story |
| `--no-ci-watch` | CI-watch polling per story PR |

## Error Envelope

| Exit | Code | Condition |
|------|------|-----------|
| 1 | `ARGS_INVALID` | Args normalizer exit 1 |
| 2 | `EPIC_DIR_MISSING` | `ai/epics/epic-XXXX/` absent |
| 3 | `STORY_FAILED` | Story returned `status=FAILED` |
| 4 | `INTEGRITY_GATE_FAILED` | Phase 4 `passed=false` after recovery |
| 5 | `FINAL_PR_CONFLICTS` | Phase 5 develop-sync conflict |
| 6 | `BRANCH_ENSURE_FAILED` | Phase 2 non-zero exit |
| 7 | `PLAN_BUILD_FAILED` | Phase 1 non-zero (non-cyclic) |
| 8 | `CYCLIC_DEPENDENCY` | Phase 1 exit 3 |

## Knowledge Pack References

Read src/main/resources/targets/claude/knowledge/lifecycle/task-hierarchy.md
Read src/main/resources/targets/claude/knowledge/governance/tool-call-grammar.md

## Full Protocol

> Per-phase detail (Phase 3 retry/backoff/circuit-breaker §2, Phase 4 integrity-gate recovery §3, Phase 5 TTY-detection + gate menu §4), resume workflow (§5), `SubagentResult` error shape (§6), `--auto-approve-pr` propagation (§7), and `args-schema.json` reference (§1) in [`references/full-protocol.md`](references/full-protocol.md). Idempotency contract and integration notes also in references.
