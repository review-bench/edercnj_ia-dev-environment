---
name: x-cleanup-git-branches
description: "Cleans local git: prune, remove non-main worktrees, delete branches except main/develop."
user-invocable: true
allowed-tools: Bash, Read
argument-hint: "[--dry-run] [--yes]"
context-budget: medium
requires-capabilities: []
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

# Skill: Local Git Cleanup (slim — ADR-0012)

## Purpose

Centralizes the "reset local git state" workflow for {{PROJECT_NAME}}: after a batch of merged PRs, stale worktrees and orphan local branches accumulate. This skill does the combined pass in one invocation — fetch with prune, remove every non-main worktree, delete every local branch outside the protected set (`main`, `master`, `develop`, plus `epic/*` and `docs/*` with open PR).

Destructive by design (the user explicitly asked for a sweep). Safety comes from the confirmation gate (`y/N`) before any deletion and from `--dry-run` preview mode.

## When to Use

- After merging a batch of feature PRs — flush remote-tracking refs and delete the merged locals.
- Before starting a large refactor — clean slate local branches / worktrees.
- When `git worktree list` or `git branch -l` gets noisy.
- NOT for surgical removal — use `/x-manage-worktrees remove --id <id>` or `git branch -D <name>` directly.

## Triggers

- `/x-cleanup-git-branches` — execute with interactive confirmation
- `/x-cleanup-git-branches --dry-run` — preview candidates, no changes
- `/x-cleanup-git-branches --yes` — execute, skip confirmation (CI / scripted use)

## Parameters

| Flag | Type | Required | Default | Description |
|------|------|----------|---------|-------------|
| `--dry-run` | Boolean | No | `false` | Preview candidate worktrees and branches without deleting. Mutually exclusive with `--yes`. |
| `--yes` / `-y` | Boolean | No | `false` | Skip the `y/N` confirmation gate. For CI / scripted use. Mutually exclusive with `--dry-run`. |

## Protected Set (Hard-Coded)

| Name | Why protected |
|------|---------------|
| `main` | Production branch (Rule 09) |
| `master` | Legacy production alias |
| `develop` | Integration branch (Rule 09) |
| `epic/*` | Always protected until the manual epic-to-develop PR gate merges (Rule 21) |
| `docs/*` with open PR | Preserved until the PR is merged or closed (EPIC-0065 D-R6); fail-safe when `gh` is absent |

The currently checked-out branch (HEAD) is **NOT** in the protected set. If HEAD points at a candidate branch, the skill checks out `develop` (fallback: `main`) before deletion.

## Output Contract

Exit codes:

| Exit | Condition |
|------|-----------|
| 0 | Success (cleanup completed, dry-run complete, nothing to clean, or user declined confirmation) |
| 1 | Operational failure (`NOT_A_REPO`, `IN_WORKTREE_UNSAFE`, `NO_SAFE_FALLBACK_BRANCH`) |
| 2 | Usage error (unknown flag, `--dry-run` and `--yes` both set) |

Stdout: human-readable cleanup plan + summary line "Worktrees removed: N / Branches deleted: M". Stderr: WARNING lines for individual failures (which do NOT abort the run).

## Workflow Overview

```text
 1. PARSE_FLAGS       -> validate --dry-run / --yes mutual exclusion
 2. DETECT_CONTEXT    -> abort with IN_WORKTREE_UNSAFE if running inside any linked worktree
 3. RESOLVE_HEAD      -> capture current branch (empty if detached)
 4. FETCH             -> git fetch --prune origin (skip if no origin)
 5. ENUM_WORKTREES    -> list non-main worktrees via git worktree list --porcelain
 6. ENUM_BRANCHES     -> local branches minus protected (main/master/develop/epic/*/docs-with-PR)
 7. PRINT_PLAN        -> human-readable candidate table; exit 0 if --dry-run or empty
 8. CONFIRM_GATE      -> y/N prompt unless --yes / --dry-run
 9. SWITCH_IF_NEEDED  -> checkout develop/main if HEAD is a candidate
10. REMOVE_WORKTREES  -> git worktree remove --force + git worktree prune
11. DELETE_BRANCHES   -> git branch -D per candidate
12. REPORT_SUMMARY    -> counts to stdout; exit 0
```

Full bash blocks for all 12 steps, the 3-classifier `detect_worktree_context()` function (Rule 14 non-nesting invariant + git-dir suffix check + toplevel-vs-main comparison), heredoc-based candidate iteration (handles paths with spaces), epic/* and docs/* filtering with `gh pr list` fallback, and `--force` worktree-removal semantics live in [`references/full-protocol.md`](references/full-protocol.md):

- **Step 1** (§Step 1): `case` loop parsing; mutual-exclusion check; usage banner.
- **Step 2** (§Step 2): 3-classifier `detect_worktree_context()` function with JSON output; abort on `inWorktree=true`.
- **Steps 3–4** (§Step 3/4): current-branch capture (empty on detached HEAD); `origin`-presence check; fail-open fetch.
- **Step 5** (§Step 5): porcelain output parsing with `sed -n 's/^worktree //p'` to preserve paths-with-spaces; `tail -n +2` to skip the main worktree.
- **Step 6** (§Step 6): `for-each-ref` enumeration; `epic/*` unconditional skip; `docs/*` with-PR check via `gh pr list --head ... --state open` (fail-safe when `gh` is absent).
- **Steps 7–8** (§Step 7/8): plan rendering; `--dry-run` early exit; interactive `y/N` gate.
- **Step 9** (§Step 9): candidate-HEAD detection; checkout `develop` then `main` fallback; `NO_SAFE_FALLBACK_BRANCH` abort.
- **Steps 10–11** (§Step 10/11): `while IFS= read -r` over heredoc; per-failure WARNING without aborting; `git worktree prune` after the loop.
- **Step 12** (§Step 12): summary printf.
- **Security & Safety Notes** (§Security & Safety Notes): blast radius is local-only; uncommitted work in worktrees is discarded by design after confirmation; protected-set regex is literal.

## Error Handling

| Scenario | Action |
|----------|--------|
| `--dry-run` and `--yes` both set | Abort with exit 2 (usage error) |
| Unknown flag passed | Abort with exit 2 (usage error) |
| Running inside any linked worktree | Abort with `IN_WORKTREE_UNSAFE`, exit 1 |
| Not a git repo | Abort with `NOT_A_REPO`, exit 1 |
| `origin` remote missing | Warn, skip fetch, continue |
| HEAD is a candidate and neither `develop` nor `main` exists | Abort with `NO_SAFE_FALLBACK_BRANCH`, exit 1 |
| Individual worktree remove fails | Warn, continue, count excludes it |
| Individual branch delete fails | Warn, continue, count excludes it |
| No candidates (worktrees + branches empty) | Print "Nothing to clean", exit 0 |

## Related Skills

| Skill | Relationship |
|-------|-------------|
| `x-manage-worktrees` | `cleanup` operation is scoped to `.claude/worktrees/*` with MERGED/STALE/ORPHAN criteria; this skill is broader and unconditional. |
| `x-push-branch` | Typical upstream action after cleanup (push a fresh branch). |
| `x-commit-changes` | Used to author commits — unrelated to cleanup, referenced here only for context. |

## References

- [Rule 09 — Branching Model](../../../rules/09-branching-model.md): protected branches policy
- [Rule 14 — Worktree Lifecycle](../../../rules/14-worktree-lifecycle.md): non-nesting invariant driving Step 2
- [x-manage-worktrees/SKILL.md](../x-manage-worktrees/SKILL.md): source of the canonical `detect_worktree_context()` snippet

## Full Protocol

Minimum viable contract above. Detailed bash for all 12 steps (3-classifier worktree detection, heredoc-based candidate iteration handling paths with spaces, epic/docs filtering, `--force` worktree removal semantics, HEAD-switch logic), error tables, and security notes live in [`references/full-protocol.md`](references/full-protocol.md) per ADR-0012 (skill body slim-by-default).
