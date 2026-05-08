# Orchestrator-wide error matrix

> Canonical error codes and recovery actions shared by skills that orchestrate multi-phase workflows:
> `x-orchestrate-epic`, `x-implement-epic`, `x-implement-story`, `x-create-feature`, `x-release`, and similar.
>
> Skill-specific rows (e.g., "Story not in implementation map", "DoR verdict cannot be extracted") live in each skill's own `## Error Handling`. The rows below apply to **every** orchestrator.

## Canonical abort codes

| Abort code | Origin | Trigger | Recovery |
|------------|--------|---------|----------|
| `WORKTREE_FAILED` | `x-manage-worktrees` | Worktree creation/detection fails non-recoverably | Inspect git worktree state; resolve manually; re-run orchestrator |
| `EPIC_BRANCH_ENSURE_FAILED` | `x-internal-ensure-epic-branch` | Cannot create or sync `epic/<ID>` branch with origin | Repair remote state (auth, permission, conflict); re-run |
| `COMMIT_FAILED` | `x-commit-planning` (exit 4) | Wave-level commit conflict or pre-commit hook failure | Inspect conflict; resolve; re-run with `--resume` |
| `PR_CREATE_FAILED` | `x-create-pr` | `gh pr create` non-zero (auth, base branch missing, duplicate) | Verify gh auth + base; re-run |
| `CI_FAILED` | `x-watch-pr-ci` (exit 20) | CI run reported failure on the PR | Read CI logs; fix code; push update; re-run |
| `GATE_SCHEMA_INVALID` | `x-internal-verify-phase-gates` | State file schema mismatch on `--resume` | Start gate fresh (delete `execution-state.json`) and re-run |

## Fail-open vs. fail-closed conventions

| Step | Convention | Rationale |
|------|------------|-----------|
| `x-manage-worktrees detect-context` (advisory) | **Fail-open** — log WARNING, continue | RULE-006: detection is not authoritative; `x-internal-ensure-epic-branch` makes the binding decision |
| `x-internal-precheck-worktree` (exit 15) | **Fail-open** — log WARNING, continue with caution | Pre-check is heuristic, not blocking |
| `x-internal-ensure-epic-branch` failure | **Fail-closed** — abort with `EPIC_BRANCH_ENSURE_FAILED` | Branch state must be canonical before any artifact is written |
| `x-commit-planning` `noOp=true` (exit 0) | **Silent no-op** — proceed to next step | Nothing to commit; not a failure |
| `x-commit-planning` exit 4 | **Fail-closed** — abort with `COMMIT_FAILED` | Wave commit cannot be skipped; inconsistent state otherwise |
| `x-push-branch` failure | **Soft fail** — WARN only; local commits preserved | Operator can `git push` manually; pushing is recoverable |
| Subagent fails for one work item (story/task) | **Mark item as `NOT_READY`/`FAILED`, continue with siblings** | Partial progress beats total restart; operator resumes affected items |

## `--dry-run` semantics

When `--dry-run` is set, the orchestrator MUST treat as **no-ops**:

- All `Skill(...)` calls that mutate the working tree, branches, or remote (Steps P1, P2, P4, P5, P6, P7 — exact set varies per orchestrator).
- All `git push`, `gh pr create`, `gh pr merge` invocations.

Artifacts (planning/review/report markdown) MAY still be written under `ai/epics/<ID>/` so the operator can preview them. The orchestrator MUST log `[DRY-RUN]` prefix on every skipped step and the final summary MUST list every skipped action so the difference between dry-run and real-run is auditable.

## Telemetry on failure

Every abort path MUST emit a `phase.end <skill> <phase> error <abort-code>` event before returning, so `events.ndjson` reflects the failure. Aborts without a telemetry close-out poison downstream `/x-analyze-telemetry` reports.
