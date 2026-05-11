---
name: x-merge-pr
description: "Merges a single PR via gh CLI with configurable strategy and idempotent behavior."
user-invocable: true
allowed-tools: Bash, Read, Write
argument-hint: "--pr N [--strategy merge|squash|rebase] [--delete-branch true|false] [--auto] [--wait-timeout-min N]"
requires-capabilities: []
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers. Start directly with technical information.
- **Progress lines**: Prefix with the current phase, e.g. `[Phase 0]`, `[Phase 1]`, `[Phase 2]`.

# Skill: Merge PR (slim — ADR-0012)

## Purpose

Merge a single pull request through the GitHub CLI with three orthogonal strategies (`merge` / `squash` / `rebase`), producing a structured result envelope and a deterministic exit code. The skill operates in two mutually exclusive modes:

- **Synchronous mode (default)** — validates pre-conditions (`mergeable`, `reviewDecision`, `statusCheckRollup`) before invoking `gh pr merge`. On success, returns the merge SHA immediately.
- **Auto-merge mode (`--auto`)** — delegates waiting to GitHub's native auto-merge (`gh pr merge --auto`). Returns in under 5 seconds; GitHub merges asynchronously once CI and approvals are green.

Idempotent: a second invocation on an already-merged PR returns `merged=true` without error. Replaces the ~80 lines of ad-hoc `gh pr merge` + polling logic previously embedded in `x-implement-epic` Phase 1.3b (story-0049-0003).

## Triggers

```
/x-merge-pr --pr 123
/x-merge-pr --pr 123 --strategy squash
/x-merge-pr --pr 123 --strategy rebase --delete-branch false
/x-merge-pr --pr 123 --strategy merge --auto
/x-merge-pr --pr 123 --wait-timeout-min 30
```

## Parameters

| Flag | Type | Required | Default | Description |
| :--- | :--- | :--- | :--- | :--- |
| `--pr` | `Integer` (> 0) | Yes | — | Target PR number. Must exist and be visible to the current `gh` token. |
| `--strategy` | `Enum` (`merge`\|`squash`\|`rebase`) | No | `merge` | Maps 1-to-1 to `gh pr merge --merge`/`--squash`/`--rebase`. |
| `--delete-branch` | `Boolean` | No | `true` | If `true`, appends `--delete-branch` to `gh pr merge`. |
| `--auto` | Flag | No | `false` | Enable GitHub native auto-merge. Skill returns in <5s regardless of CI state. |
| `--wait-timeout-min` | `Integer` (1–600) | No | `60` | Synchronous-mode timeout in minutes for `mergeable=UNKNOWN` polling. Ignored when `--auto` is set. |

Validation details (mutual exclusivity, range checks, `--auto`+`--wait-timeout-min` independence) in [`references/full-protocol.md`](references/full-protocol.md) §Mutual Exclusivity.

## Output Contract

### Response Envelope (stdout, single-line JSON)

| Field | Type | Always | Description |
| :--- | :--- | :--- | :--- |
| `merged` | `Boolean` | Yes | `true` when PR is MERGED at skill exit (sync mode). `false` in `--auto` mode. |
| `mergeSha` | `String(40)` | sync success only | SHA of merge commit from `gh pr view`. Absent on error or `--auto`. |
| `prState` | `Enum` (`OPEN`\|`MERGED`\|`CLOSED`) | Yes | Final observed PR state. |
| `waitedSec` | `Integer` | Yes | Seconds waited for state transition (0 when idempotent or `--auto`). |
| `autoEnabled` | `Boolean` | Yes | `true` when GitHub auto-merge was enabled by this invocation. |

Example success (synchronous):

```json
{"merged":true,"mergeSha":"a1b2c3d4...","prState":"MERGED","waitedSec":12,"autoEnabled":false}
```

Example success (auto mode):

```json
{"merged":false,"prState":"OPEN","waitedSec":0,"autoEnabled":true}
```

Example idempotent (already merged):

```json
{"merged":true,"prState":"MERGED","waitedSec":0,"autoEnabled":false}
```

### Exit Codes

| Exit | Error Code | Condition |
| :--- | :--- | :--- |
| 0 | — | Success (merge happened, auto enabled, or idempotent no-op) |
| 1 | `PR_NOT_FOUND` / `INVALID_STRATEGY` / `INVALID_TIMEOUT` | PR missing or input invalid |
| 2 | `PR_CLOSED` | PR state is `CLOSED` without a merge |
| 3 | `NOT_APPROVED` | Sync mode: `reviewDecision != APPROVED` |
| 4 | `CI_FAILING` | Sync mode: any failing check in `statusCheckRollup` |
| 5 | `MERGE_CONFLICT` | `mergeable == CONFLICTING` |
| 6 | `TIMEOUT` | Sync mode: `mergeable == UNKNOWN` past `--wait-timeout-min` |

Exit codes are stable contract — consumers (`x-implement-epic`, `x-create-pr --auto-merge`) MAY switch on them.

## Workflow Overview

```text
Phase 0: PARSE          -> normalize flags, reject invalid input
Phase 1: STATE_INSPECT  -> single gh pr view; exit early on MERGED/CLOSED (idempotency)
Phase 2: MODE_DISPATCH  -> branch on --auto vs synchronous
Phase 3a: AUTO_MERGE    -> gh pr merge --auto; return in <5s; handle "clean state" edge case
Phase 3b: SYNC_MERGE    -> pre-checks (mergeable/review/checks); gh pr merge; fetch mergeSha
Phase 4: EMIT           -> single-line JSON to stdout; exit with code
```

Each phase's full bash, decision tables, and edge cases live in [`references/full-protocol.md`](references/full-protocol.md):

- **Phase 0** (§Phase 0): flag parsing with `case` loop; `--strategy`/`--wait-timeout-min` enumeration validation; informational warning when `--wait-timeout-min` set alongside `--auto`.
- **Phase 1** (§Phase 1.1–1.2): single `gh pr view` call with full `--json` field list; idempotency decision table (`MERGED` → exit 0, `CLOSED` → exit 2, `OPEN` → continue).
- **Phase 2** (§Phase 2): single `--auto` flag check; no I/O.
- **Phase 3a** (§Phase 3a.1–3a.2): `gh pr merge --auto` flag composition; "clean state" edge case (synchronous fallback with state refetch); "auto-merge is not allowed" repo-misconfiguration handling.
- **Phase 3b** (§Phase 3b.1–3b.6): three sequential pre-checks (`mergeable`, `reviewDecision`, `statusCheckRollup`) with 10-second polling loop for `UNKNOWN`/`IN_PROGRESS`; `gh pr merge` invocation; `mergeSha` fetch with 2s retry on rare null race.
- **Phase 4** (§Phase 4): stdout-vs-stderr separation contract (stdout = single JSON envelope, stderr = `[Phase N] ...` logs); error envelope shape with `errorCode` + `message`.
- **Worked examples 1–5** (§Worked Examples): happy path (merge), squash + auto, rebase + preserve branch, idempotent re-invocation, NOT_APPROVED error.

## Error Envelope

| Scenario | Exit | Error Code |
| :--- | :--- | :--- |
| `--pr` missing, zero, negative, non-numeric | 1 | `PR_NOT_FOUND` |
| `--strategy` not in `{merge, squash, rebase}` | 1 | `INVALID_STRATEGY` |
| `--wait-timeout-min` not in `[1, 600]` | 1 | `INVALID_TIMEOUT` |
| `gh pr view` returns "not found" | 1 | `PR_NOT_FOUND` |
| PR state is `CLOSED` (not merged) | 2 | `PR_CLOSED` |
| `reviewDecision != APPROVED` (sync mode) | 3 | `NOT_APPROVED` |
| `statusCheckRollup` has failing checks (sync mode) | 4 | `CI_FAILING` |
| `mergeable == CONFLICTING` | 5 | `MERGE_CONFLICT` |
| `mergeable == UNKNOWN` past timeout (sync mode) | 6 | `TIMEOUT` |

## Integration Notes

| Consumer | Relationship | Usage |
| :--- | :--- | :--- |
| `x-create-pr` | invokes | When `--auto-merge <strategy>` is passed (RULE-002 auto-merge default ON), forwards `--strategy` + `--auto` to enable native GitHub auto-merge. |
| `x-implement-epic` | invokes | Phase 1.3b merge of epic PRs to `epic/XXXX` or `develop`. Replaces ~80 lines of inline `gh pr merge` + polling. Calls with `--strategy merge --auto` by default. |
| `x-manage-pr-merge-train` | shares contract | Uses its own train-specific merge orchestration; shares exit-code semantics for consistency across `/x-pr-*` family. |

Called tools: `gh pr view` (1–2 calls), `gh pr merge` (1 call per path), `sleep` (only in polling loops). No `git` operations — branch deletion delegated to `gh pr merge --delete-branch`.

## Forbidden

- NEVER invoke `git merge` directly — all merges MUST route through `gh pr merge` to preserve GitHub-side history and branch-deletion side effects.
- NEVER bypass pre-checks in synchronous mode — even on repeat invocations after a CI flake.
- NEVER emit multiple JSON envelopes to stdout — callers rely on single-line parsing.
- NEVER exit 0 on any error condition — the exit-code table is a hard contract.
- NEVER poll `gh pr merge` itself — only `gh pr view` is polled. `gh pr merge` is a single synchronous call per phase.

## Status

- **Story**: story-0049-0003 (epic-0049, "x-merge-pr extraction from x-implement-epic Phase 1.3b")
- **Parent Contract**: RULE-002 (auto-merge default ON), RULE-004 (preserve history), RULE-005 (thin orchestrator)
- **Downstream**: story-0049-0016 (`x-create-pr --auto-merge` propagation), story-0049-0018 (`x-implement-epic` refactor)

## Full Protocol

Minimum viable contract above. Detailed step-by-step bash for all 6 phases, 5 worked examples (happy paths + error cases), 15 unit test scenarios + smoke test contract, coverage targets, and mutual-exclusivity validation rules live in [`references/full-protocol.md`](references/full-protocol.md) per ADR-0012 (skill body slim-by-default).
