---
name: x-watch-pr-ci
description: "Polls a PR's CI checks and Copilot review until completion or timeout (8 exit codes)."
user-invocable: true
allowed-tools: Bash
argument-hint: "--pr-number <N> [--timeout-seconds 1800] [--poll-interval-seconds 60] [--require-copilot-review true] [--require-checks-passing true] [--copilot-review-timeout 900] [--state-file <path>] [--no-state-file]"
requires-capabilities: []
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

# Skill: x-watch-pr-ci (slim — ADR-0012)

## Purpose

Polls a pull request's CI checks and Copilot review until checks complete (or timeout), producing a stable exit code that orchestrators use to build the interactive gate menu (EPIC-0043).

Solves the gap identified in `spec-ci-watch.md §2`: when `x-fix-pr` is invoked from the `FIX-PR` slot it finds zero comments because Copilot hasn't posted yet (review typically takes 30–180s). `x-watch-pr-ci` encapsulates the wait so every caller receives real feedback before presenting a decision gate.

## Triggers

- `/x-watch-pr-ci --pr-number 42` — watch PR #42 with defaults
- `/x-watch-pr-ci --pr-number 42 --timeout-seconds 600` — custom timeout
- `/x-watch-pr-ci --pr-number 42 --require-copilot-review false` — skip Copilot wait
- `/x-watch-pr-ci --pr-number 42 --no-state-file` — fire-and-forget (no state persistence)

## Parameters

| Parameter | Type | Default | Bounds | Description |
|-----------|------|---------|--------|-------------|
| `--pr-number <N>` | int | — (required) | >0 | PR number to monitor. |
| `--timeout-seconds <N>` | int | 1800 | 60–7200 | Global timeout. |
| `--poll-interval-seconds <N>` | int | 60 | 15–300 | Sleep between polls. |
| `--require-copilot-review` | boolean | true | — | Wait for `copilot-pull-request-reviewer[bot]`. |
| `--require-checks-passing` | boolean | true | — | Require all checks `success` (not just neutral/skipped). |
| `--copilot-review-timeout <N>` | int | 900 | 60–timeout | Copilot-specific sub-timeout. |
| `--state-file <path>` | path | `.claude/state/pr-watch-<N>.json` | 512 chars | State file for resume. |
| `--no-state-file` | flag | — | — | Disable state persistence. |

## Exit Codes (Stable Public Contract — RULE-045-05)

| Code | Name | Condition |
|------|------|-----------|
| 0 | `SUCCESS` | All checks green + Copilot review present (or `--require-copilot-review=false`). |
| 10 | `CI_PENDING_PROCEED` | Checks green + Copilot timeout elapsed without review. Proceed with caution. |
| 20 | `CI_FAILED` | A check concluded with `failure`, `timed_out`, `cancelled`, or `action_required`. |
| 30 | `TIMEOUT` | Global timeout elapsed with checks still pending. |
| 40 | `PR_ALREADY_MERGED` | PR was already merged — idempotent exit. |
| 50 | `NO_CI_CONFIGURED` | `statusCheckRollup` is empty — no CI configured. |
| 60 | `PR_CLOSED` | PR closed without merge. |
| 70 | `PR_NOT_FOUND` | PR does not exist or caller lacks permission. |

These codes are a **public contract**. Adding a new code = MINOR bump; changing semantics = MAJOR bump (Rule 08 — SemVer).

## Output Contract (Stdout)

The final JSON summary is the **last line** on **stdout**. Progress logs go to **stderr**.

```json
{
  "status": "SUCCESS",
  "prNumber": 42,
  "checks": [
    {"name": "build", "conclusion": "success"},
    {"name": "test",  "conclusion": "success"}
  ],
  "copilotReview": {"present": true, "reviewId": 12345678},
  "elapsedSeconds": 87
}
```

State file (when not disabled) at `.claude/state/pr-watch-<N>.json` enables resume after session restart (atomic write via `.tmp` + rename).

## Invocation by Orchestrators (Rule 13 INLINE-SKILL)

```markdown
Skill(skill: "x-watch-pr-ci", args: "--pr-number 42")
```

Orchestrators MUST use this Pattern 1 INLINE-SKILL form. Bare-slash (`/x-watch-pr-ci`) is forbidden in delegation contexts (Rule 13 §Forbidden).

## Workflow Overview

```text
1. VALIDATE   -> Parse and validate arguments; reject out-of-bounds values
2. RESUME     -> Load state-file if present (skip elapsed time already consumed)
3. POLL LOOP  -> while elapsed < timeout:
   a. gh pr view <N> --json state,mergedAt,statusCheckRollup
   b. gh api repos/{owner}/{repo}/pulls/{N}/reviews (Copilot review check)
   c. Classify (PR state + failing checks + all-green + Copilot present + elapsed)
   d. Write state-file (atomic: .tmp + rename)
   e. If terminal condition → emit JSON + exit with stable code
   f. Sleep poll-interval-seconds
4. TIMEOUT    -> emit JSON + exit 30
```

Full bash for all 3 steps, `emit_json` helper, state-file schema (RULE-045-03), stdout/stderr separation contract, edge cases (rate-limit exponential backoff, Copilot-not-configured, corrupted state, mid-poll merge/close), and rule compliance live in [`references/full-protocol.md`](references/full-protocol.md):

- **Step 1** (§Step 1): bounds validation per flag with specific exit codes.
- **Step 2** (§Step 2): state-file resume via `jq -r .startedAt` + `ELAPSED_OFFSET` computation; `mkdir -p` on state directory.
- **Step 3** (§Step 3): polling loop with `gh pr view` + `gh api .../reviews`; rate-limit detection with exponential backoff (30s/60s/120s, max 3 retries → exit 30); early-exit classification (MERGED→40, CLOSED→60, empty CI→50, failing→20, all-green+Copilot→0, all-green+Copilot-timeout→10).
- **Helper** (§Helper emit_json): JSON envelope construction via `jq -n` with all 5 fields.
- **State File Schema** (§State File Schema): RULE-045-03 schema v1.0 with 7 fields; atomic write via `.tmp` + `mv`.
- **Stdout Contract** (§Stdout Contract): final-line JSON contract; stderr for progress.
- **Edge Cases** (§Edge Cases): 5-row table covering rate-limit, no-Copilot-config, corruption, mid-poll merge/close.
- **Rule Compliance** (§Rule Compliance): Rule 13 (INLINE-SKILL pattern), Rule 14 (no worktree creation), RULE-045-03/04/05/06.

## Knowledge Pack References

| Pack | File | Purpose |
|------|------|---------|
| lifecycle | `.claude/knowledge/lifecycle/execution-integrity.md` | EPIC-0043 interactive-gate contract |
| lifecycle | `.claude/knowledge/lifecycle/ci-watch-integrity.md` | Reasoning behind 8 exit codes |

## Full Protocol

Minimum viable contract above. Detailed bash for argument validation, state-file resume, polling loop with rate-limit handling and 8-state classification, `emit_json` helper, state-file schema, stdout/stderr separation, edge cases, and rule compliance live in [`references/full-protocol.md`](references/full-protocol.md) per ADR-0012 (skill body slim-by-default).
