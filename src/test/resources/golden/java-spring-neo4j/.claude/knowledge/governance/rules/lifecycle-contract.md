---
name: lifecycle-contract
description: Consolidated lifecycle integrity contract — flowVersion, 5 invariants, enforcement layers, bypass exceptions, refinement gate, CI-watch
requires-capabilities: []
---
# Lifecycle Integrity Contract — Full Reference

> **Introduced by:** EPIC-0049. **Extended by:** EPIC-0055, EPIC-0059, EPIC-0061, EPIC-0068, EPIC-0069, EPIC-0077.
> **Full fallback matrices, 12 surfaces, personas, 8 CI-Watch exit codes:**
> `Read .claude/knowledge/lifecycle/backward-compatibility.md`
> `Read .claude/knowledge/lifecycle/execution-integrity.md`
> `Read .claude/knowledge/lifecycle/zero-bypass.md`
> `Read .claude/knowledge/lifecycle/refinement-gate.md`
> `Read .claude/knowledge/lifecycle/ci-watch-integrity.md`

## `flowVersion` Quick Reference

| Value | Semantics |
| :--- | :--- |
| `"1"` | Legacy flow — story PRs → develop; no epic branch |
| `"2"` | Story PRs → epic/XXXX; task tracking required |
| `"3"` | EPIC-0061 Local-First — non-interactive default |
| `"4"` | v4 layout: `ai/epics/<epic-slug>/` via PathResolver |
| `"5"` | EPIC-0077 Product-First — `productFirstLifecycle: true` |

Field absent → defaults to `"1"` (legacy) with WARNING. See `backward-compatibility.md` for the full fallback matrix including `interactiveMode`, `taskTracking`, `refinementVerdict`, and `productFirstLifecycle`.

## Invariants (5 — Must Hold Before Any PR Merge)

1. **`flowVersion` resolved.** Valid `flowVersion ∈ {"1","2","3","4","5"}`. `flowVersion="2"` requires `taskTracking.enabled=true` (hard fail: `TASK_TRACKING_REQUIRED`).
2. **Evidence present.** All mandatory artifacts exist for merged stories. Absent → `EIE_EVIDENCE_MISSING` (Camada 3 CI audit).
3. **`refinementVerdict.status = "approved"` before implement.** `enforce-refinement-gate.sh` (Camada 0) blocks with exit 33 `REFINEMENT_REQUIRED` when not approved. Exception: `flowVersion=1` and `hotfix/*` branches.
4. **CI-watch ran per PR.** `.claude/state/pr-watch-{PR}.json` state file must exist for every merged PR. Absent → WARNING (Camada 2).
5. **Orchestrator invoked.** Every story/task must be traceable to `x-implement-story`/`x-implement-task`. No manual `git commit + gh pr create` bypass.

## Enforcement Layers (5 Total)

| Camada | Mechanism | Mode |
| :--- | :--- | :--- |
| 0 | PreToolUse hooks (`enforce-preflight-gates.sh`, `enforce-refinement-gate.sh`) | **Preventive** |
| 1 | Rules + CLAUDE.md + SKILL.md MANDATORY markers | Normative |
| 2 | Stop hook `verify-story-completion.sh` | Detectivo (runtime) |
| 3 | CI audit (`audit-execution-integrity.sh`, `audit-refinement-gate.sh`, `audit-flow-version.sh`) | Detectivo (CI) |
| 4 | Telemetry NDJSON (`events.ndjson`) | Observability |

## Bypass Exceptions (3 Only)

1. **`--legacy-flow`** for `flowVersion=1` epics created before EPIC-0049 merges.
2. **`hotfix/*` branches** — single-file critical fix with `## Hotfix Bypass Justification` in PR body.
3. **`CLAUDE_RECOVERY_MODE=1`** — allows `--skip-review` and `--no-ci-watch` only; NEVER bypasses refinement gate.

No other bypass path exists. Undocumented env vars (`CLAUDE_SKIP_AUDIT=1`, etc.) are blocked by `enforce-no-bypass-flags.sh`.

## Refinement Gate Contract

Every story/epic MUST have `refinementVerdict.status = "approved"` before `x-implement-story`, `x-implement-epic`, `x-implement-task`, or `x-orchestrate-epic` is invoked.

Gate enforced at:
- Camada 0: `enforce-refinement-gate.sh`, exit 33 `REFINEMENT_REQUIRED`
- Camada 2: `audit-refinement-gate.sh`, exit 1 `REFINEMENT_GATE_VIOLATION`

**Forbidden:**
- Manually editing the `## Refinement Verdict` block without re-running `/x-refine-story` or `/x-refine-epic`
- Using bypass env vars for the refinement gate — `CLAUDE_RECOVERY_MODE=1` does not bypass it

## CI-Watch Contract

Every orchestrator that creates a PR via `x-create-pr` MUST follow with `Skill(skill: "x-watch-pr-ci", args: "--pr-number <PR>")`. The `.claude/state/pr-watch-{PR}.json` state file IS the evidence.

### CI-Watch Exit Codes (8 Stable)

`0=SUCCESS`, `10=CI_PENDING_PROCEED`, `20=CI_FAILED`, `30=TIMEOUT`, `40=PR_ALREADY_MERGED`, `50=NO_CI_CONFIGURED`, `60=PR_CLOSED`, `70=PR_NOT_FOUND`.

**Forbidden:**
- Inlining `gh pr checks` instead of invoking `x-watch-pr-ci`
- Using `--no-ci-watch` outside `## Recovery` blocks
- Hard-coding numeric exit codes

## Execution Integrity Contract

Every `Skill(skill: "...", args: "...")` in a SKILL.md body MUST be executed as a real tool call — never simulated, summarized, or skipped without an explicit `--skip-*` flag inside a `## Recovery` block.

**Forbidden:**
- Inlining what a sub-skill would do instead of emitting the Skill tool call
- Using `--skip-*` flags outside `## Recovery` blocks
- Bypassing `scripts/audit-execution-integrity.sh` via `--no-verify`

## Zero-Bypass Contract

Every story MUST be traceable to `x-implement-story`; every task to `x-implement-task`. No PR targeting `epic/*` or `develop` may be merged without all 13 surface evidence artifacts present.

**Forbidden:**
- Direct `git commit` + `gh pr create` without invoking the appropriate orchestrator
- Marking a story `COMPLETE` without the full evidence artifact set

## Skill Renaming

Old names remain in dispatch table with `DEPRECATED` warning for **one release** after rename. **Hard-cut** is authorized for visibility changes, taxonomic merges, or semantic redefinitions — document under `## Removed` in CHANGELOG.
