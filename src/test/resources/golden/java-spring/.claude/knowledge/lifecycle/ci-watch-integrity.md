---
name: kp-lifecycle-ci-watch-integrity
description: "Full reference for Rule 45 CI-Watch Integrity: 8 exit codes matrix, fallback matrix for CI environment misbehavior, --no-ci-watch constraints, mandatory invocation sites, and audit contract."
requires-capabilities: []
---

# Knowledge Pack: CI-Watch Integrity (Rule 45 — Full Reference)

## Exit Codes Matrix (8 Stable Codes — RULE-045-05)

| Exit | Code | Condition | Suggested action |
| :--- | :--- | :--- | :--- |
| 0 | `SUCCESS` | All CI checks green AND Copilot review present (or `--require-copilot-review=false`). | Proceed with merge. |
| 10 | `CI_PENDING_PROCEED` | All checks green BUT Copilot review timeout elapsed without review. | Proceed with caution; surface WARNING. |
| 20 | `CI_FAILED` | At least one check returned `failure`, `timed_out`, `cancelled`, or `action_required`. | Block merge; route to `x-fix-pr` or FIX-PR slot. |
| 30 | `TIMEOUT` | Global polling timeout elapsed with checks still pending. | Surface to operator; offer ABORT / extend / proceed. |
| 40 | `PR_ALREADY_MERGED` | PR was already merged before / during polling — idempotent exit. | Treat as SUCCESS; no further action. |
| 50 | `NO_CI_CONFIGURED` | `statusCheckRollup` is empty — no CI configured for this PR. | Skip CI gate; rely on review-only. |
| 60 | `PR_CLOSED` | PR was closed without merge during polling. | ABORT; require operator decision. |
| 70 | `PR_NOT_FOUND` | PR does not exist or caller lacks permission. | Fail-fast; investigate auth / PR id. |

Adding a new code is a MINOR version bump; changing semantics of an existing code is a MAJOR version bump (Rule 08 — SemVer).

## Fallback Matrix (CI Environment Misbehavior)

| Scenario | Resolved exit code | Rationale |
| :--- | :--- | :--- |
| Repository has zero check runs | `50` (`NO_CI_CONFIGURED`) | Fail-open — caller decides whether to proceed. |
| Copilot bot not a reviewer | `0` if other checks green AND `--require-copilot-review=false`; else `10` after timeout | Copilot absence is operational, not a CI failure. |
| `gh pr view` fails with non-zero exit | `70` (`PR_NOT_FOUND`) after one retry | Auth/network failures = PR unreachable. |
| Polling exceeds global timeout | `30` (`TIMEOUT`) | Operator MUST intervene; never silent-pass. |
| PR is closed (not merged) mid-poll | `60` (`PR_CLOSED`) | Stop polling; report state to caller. |
| PR was merged before first poll iteration | `40` (`PR_ALREADY_MERGED`) | Idempotent — safe to retry invocation. |

## Mandatory Invocation Sites

Every orchestrator that creates a PR via `x-create-pr` MUST follow with one `x-watch-pr-ci` invocation:

```
Skill(skill: "x-create-pr", model: "haiku", args: "...")    # creates PR
Skill(skill: "x-watch-pr-ci", args: "--pr-number <PR>")     # MANDATORY (Rule 45)
```

| Orchestrator | Phase carrying the watch step |
| :--- | :--- |
| `x-implement-task` | Step 4.5 (post `x-create-pr`) |
| `x-implement-story` | Phase 2.2.8.5 (post task PRs and story PR) |
| `x-release` | Step 8 (post release PR) |
| `x-implement-epic` | Phase 5 (post final epic-to-develop PR) |
| `x-manage-pr-merge-train` | Per-PR (after auto-merge gate) |

## `--no-ci-watch` Constraints

Permitted ONLY in two contexts:
1. **`## Recovery` blocks of a calling skill** — when recovering from partial failure and CI already validated upstream.
2. **CI / automated environments** where the surrounding pipeline already gates on the same CI signal and a second poll would deadlock.

Every `--no-ci-watch` occurrence outside these contexts is caught by `scripts/audit-bypass-flags.sh` and fails CI with `BYPASS_FLAG_VIOLATION`. Per-line escape: `<!-- audit-exempt: <reason> -->` immediately preceding.

## State File Contract

`.claude/state/pr-watch-{PR_NUMBER}.json` is produced by every `x-watch-pr-ci` invocation. This file IS the evidence the watch ran. If it does not exist, the skill was not invoked.

## Audit Contract

| Layer | Mechanism | Trigger |
| :--- | :--- | :--- |
| Camada 1 | Rule 45 + CLAUDE.md | Every conversation |
| Camada 2 | `verify-story-completion.sh` (Stop hook) | End of LLM turn — checks `.claude/state/pr-watch-{PR}.json` |
| Camada 3 | `audit-execution-integrity.sh` + `audit-bypass-flags.sh` | PR open/sync — verifies state file per merged PR |
| Camada 4 | Observability via state file existence | Continuous — file IS the proof |

Self-check: `scripts/audit-execution-integrity.sh --self-check` MUST verify this rule file exists and `x-watch-pr-ci/SKILL.md` references `RULE-045-05`.
