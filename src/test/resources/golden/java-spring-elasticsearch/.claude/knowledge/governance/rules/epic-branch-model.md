---
name: epic-branch-model
description: Full epic branch model — single integration branch per epic, lifecycle, invariants, anti-patterns, backward compatibility
requires-capabilities: []
---
# Epic Branch Model — Full Reference

> **Introduced by:** EPIC-0049
> **See also:** `.claude/knowledge/governance/rules/branching.md` for base Git Flow model

## Purpose

Every epic lives on a single long-lived integration branch `epic/XXXX`. Story PRs auto-merge into it; the final `epic/XXXX → develop` PR is a **manual gate**.

## Branch Naming

| Pattern | Example | Notes |
| :--- | :--- | :--- |
| `epic/{epic-id}` | `epic/0049` | 4-digit zero-padded integer |

- Branch names MUST be lowercase with hyphens
- The `epic/` prefix is reserved
- One and only one branch per epic ID (idempotent creation)

## Lifecycle

```
develop ──●────────────────────────────────●──────────→
          │                                ↑
          │ (birth)                        │ manual PR gate
          ↓                                │
epic/XXXX ●──●──●──●──●──●──●──●──●──●──●──●
             ↑  ↑  story PRs auto-merge in
             planning artifact commits
```

| Phase | Operation | Actor |
| :--- | :--- | :--- |
| 1. Birth | `epic/XXXX` created from `develop` | `x-internal-ensure-epic-branch` |
| 2. Planning | Planning skills commit artifacts via `x-commit-planning` | 7 planning skills |
| 3. Execution | Story PRs auto-merge into `epic/XXXX` | `x-create-pr --target-branch epic/XXXX --auto-merge` |
| 4. Gate | Manual PR `epic/XXXX → develop` | Human reviewer |
| 5. Retirement | Branch deleted after merge | `x-cleanup-git-branches` (post-merge) |

## Invariants

1. **Single source of truth.** `x-internal-ensure-epic-branch` is the only skill that creates `epic/XXXX`. All others call it.
2. **Protection from cleanup.** `x-cleanup-git-branches` MUST exclude `epic/*` from destructive sweep.
3. **Worktree base.** In `--parallel` mode, story worktrees use `epic/XXXX` as base, **not** `develop`.
4. **Auto-merge target.** Story PRs target `epic/XXXX`, never `develop`.
5. **Legacy escape hatch.** `--legacy-flow` forces all target branches back to `develop`.

## Anti-Patterns

- **Direct commit to `epic/XXXX`** — always via PR or `x-commit-planning` (restricted to `plans/**`)
- **Multiple epic branches for one epic ID** — `epic/0049`, `epic/0049-refactor` are forbidden
- **Merging `epic/XXXX` into `main`** — epic branches merge into `develop` only
- **Force-pushing `epic/XXXX`** — prohibited once any story PR has been merged
- **Skipping the manual gate** — automating `epic/XXXX → develop` defeats the purpose

**Exception — `docs/` planning PRs:** PRs from `docs/<epic-id>-<slug>` targeting `epic/XXXX` with label `docs` are auto-merged. The audit `audit-epic-branches.sh` explicitly permits this.

## Backward Compatibility

- `flowVersion="1"` or absent: legacy flow — story PRs target `develop` directly
- `--legacy-flow`: forces legacy mode (warning emitted)
- Deprecation window: 2 releases after EPIC-0049 merge

## Audit

`scripts/audit-epic-branches.sh` verifies:
- Each open `epic/*` PR has `flowVersion: "2"` in its `execution-state.json`
- No `epic/*` branch has been force-pushed after its first merge commit
- `x-cleanup-git-branches` excludes `epic/*` from protected-branch bypass

Violation → `EPIC_BRANCH_VIOLATION`.
