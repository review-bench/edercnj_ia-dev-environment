# Rule 19 — Lifecycle Integrity Contract

> **Related:** Rule 08 (Release Process), Rule 21 (Epic Branch Model), Rule 22 (Skill Visibility), Rule 24 (Execution Integrity), Rule 27 (Zero-Bypass), Rule 29 (Refinement Gate), Rule 45 (CI-Watch).
> **Introduced by:** EPIC-0049. **Extended by:** EPIC-0055, EPIC-0059, EPIC-0061, EPIC-0068, EPIC-0069, EPIC-0077.
> **Full fallback matrices, 12 surfaces, personas, 8 CI-Watch exit codes:**
> `Read src/main/resources/targets/claude/knowledge/lifecycle/backward-compatibility.md`
> `Read src/main/resources/targets/claude/knowledge/lifecycle/execution-integrity.md`
> `Read src/main/resources/targets/claude/knowledge/lifecycle/zero-bypass.md`
> `Read src/main/resources/targets/claude/knowledge/lifecycle/refinement-gate.md`
> `Read src/main/resources/targets/claude/knowledge/lifecycle/ci-watch-integrity.md`

## Purpose

Rule 19 is the canonical **Lifecycle Integrity Contract**: a single source of truth for the invariants that all orchestrators (`x-implement-epic`, `x-implement-story`, `x-implement-task`) must satisfy. It supersedes the scattered coverage of backwards compatibility and enforcement contracts previously spread across Rules 24/27/29/45. Detail lives in the 5 lifecycle KPs above.

## `flowVersion` (Quick Reference)

| Value | Semantics |
| :--- | :--- |
| `"1"` | Legacy flow — story PRs → develop; no epic branch |
| `"2"` | Story PRs → epic/XXXX; task tracking required |
| `"3"` | EPIC-0061 Local-First — non-interactive default |
| `"4"` | v4 layout: `ai/epics/<epic-slug>/` via PathResolver |
| `"5"` | EPIC-0077 Product-First — `productFirstLifecycle: true` |

Field absent → defaults to `"1"` (legacy) with WARNING. See KP `backward-compatibility.md` for the full fallback matrix for `flowVersion`, `taskTracking`, `interactiveMode`, `refinementVerdict`, and `productFirstLifecycle`.

## Invariants (5 — Must Hold Before Any PR Merge)

1. **`flowVersion` resolved.** `execution-state.json` has a valid `flowVersion ∈ {"1","2","3","4","5"}`. Absent field → legacy with warning. `flowVersion="2"` requires `taskTracking.enabled=true` (hard fail: `TASK_TRACKING_REQUIRED`).
2. **Evidence present.** All mandatory artifacts exist for merged stories (see KP `execution-integrity.md`). Absent → `EIE_EVIDENCE_MISSING` (Camada 3 CI audit).
3. **`refinementVerdict.status = "approved"` before implement.** `enforce-refinement-gate.sh` (Camada 0) blocks with exit 33 `REFINEMENT_REQUIRED` when not approved. Exception: `flowVersion=1` and `hotfix/*` branches.
4. **CI-watch ran per PR.** `.claude/state/pr-watch-{PR}.json` state file must exist for every merged PR (Rule 45). Absent → `verify-story-completion.sh` WARNING (Camada 2).
5. **Orchestrator invoked.** Every story/task implementation must be traceable to `x-implement-story`/`x-implement-task`. No manual `git commit + gh pr create` bypass. See KP `zero-bypass.md` for the 13 surfaces and 2 legitimate exception paths.

## Enforcement Layers (5 Total)

| Camada | Mechanism | Mode |
| :--- | :--- | :--- |
| 0 | PreToolUse hooks (`enforce-preflight-gates.sh`, `enforce-refinement-gate.sh`) | **Preventive** |
| 1 | This rule + CLAUDE.md + SKILL.md MANDATORY markers | Normative |
| 2 | Stop hook `verify-story-completion.sh` | Detectivo (runtime) |
| 3 | CI audit (`audit-execution-integrity.sh`, `audit-refinement-gate.sh`, `audit-flow-version.sh`) | Detectivo (CI) |
| 4 | Telemetry NDJSON (`events.ndjson`) | Observability |

## Bypass Exceptions (3 Only)

1. **`--legacy-flow`** for `flowVersion=1` epics created before EPIC-0049 merges.
2. **`hotfix/*` branches** — single-file critical fix with `## Hotfix Bypass Justification` in PR body.
3. **`CLAUDE_RECOVERY_MODE=1`** — allows `--skip-review` and `--no-ci-watch` only; NEVER bypasses refinement gate.

No other bypass path exists. Undocumented env vars (`CLAUDE_SKIP_AUDIT=1`, etc.) are blocked by `enforce-no-bypass-flags.sh`.

## Skill Renaming

Old names remain in dispatch table with `DEPRECATED` warning for **one release** after rename. **Hard-cut** (immediate removal) is authorized for visibility changes, taxonomic merges, or semantic redefinitions — must be documented under `## Removed` in CHANGELOG.

## `--legacy-flow` Flag

Forces `flowVersion: "1"` on new state files; resets target branches to `develop`; disables Rule 21 epic-branch routing. Allowed for 2-release deprecation window after EPIC-0049 merge.

## Forbidden

- Removing `flowVersion` resolution logic during the deprecation window.
- Silently upgrading `flowVersion: "1"` to `"2"` mid-execution (breaks resume).
- Shipping a new required `execution-state.json` field without a KP fallback matrix entry.
- Removing a renamed skill's old name in the same release as the rename.
