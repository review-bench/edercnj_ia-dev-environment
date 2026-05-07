---
name: branching
description: Full Git Flow branching model — branch types, naming conventions, merge direction, branch protection
requires-capabilities: []
---
# Branching Model (Git Flow) — Full Reference

> **See also:** `.claude/knowledge/governance/rules/release-process.md` for SemVer, Conventional Commits, CHANGELOG
> **See also:** `.claude/knowledge/governance/rules/epic-branch-model.md` for epic/* branch details

## Branch Types

| Branch | Purpose | Lifetime | Created From | Merges Into |
|--------|---------|----------|--------------|-------------|
| `main` | Production-ready code | Permanent | — | — |
| `develop` | Integration branch for next release | Permanent | `main` (initial) | — |
| `epic/*` | Epic integration branch | Temporary | `develop` | `develop` (manual PR gate) |
| `feature/*` | New feature development | Temporary | `develop` | `develop` |
| `release/*` | Release stabilization | Temporary | `develop` | `main` + `develop` |
| `hotfix/*` | Critical production fix | Temporary | `main` | `main` + `develop` |
| `docs/*` | Planning artifacts for feature creation | Temporary | `epic/XXXX` or `develop` | `epic/XXXX` (auto-merge, label `docs`) or `develop` (manual) |

## Naming Conventions

| Branch Type | Pattern | Examples |
|-------------|---------|----------|
| Epic | `epic/{epic-id}` | `epic/0049` |
| Feature | `feature/{ticket-id}-{short-desc}` | `feature/PROJ-123-add-auth` |
| Release | `release/{version}` | `release/1.2.0` |
| Hotfix | `hotfix/{ticket-id}-{short-desc}` | `hotfix/PROJ-456-fix-crash` |
| Bugfix | `fix/{ticket-id}-{short-desc}` | `fix/PROJ-789-null-check` |
| Docs (creation) | `docs/{epic-id}-{short-slug}` | `docs/0065-feature-create` |
| Docs (ideation) | `docs/feature-{short-slug}` | `docs/feature-csv-export` |

- Branch names MUST be lowercase with hyphens (no underscores, no camelCase)
- Maximum branch name length: 100 characters

## Merge Direction Rules

```
hotfix/*  ──→  main  ──→  (tag vX.Y.Z)
    └──────→  develop

feature/* ──→  develop

develop   ──→  release/*
release/* ──→  main  ──→  (tag vX.Y.Z)
    └──────→  develop
```

| Source | Target | Merge Strategy | Conditions |
|--------|--------|----------------|------------|
| `feature/*` → `develop` | Squash merge | All tests pass, PR approved |
| `develop` → `release/*` | Create branch | Version bumped, feature freeze |
| `release/*` → `main` | Merge commit | All tests pass, CHANGELOG updated |
| `release/*` → `develop` | Merge commit | Back-port release fixes |
| `hotfix/*` → `main` | Merge commit | Critical fix verified |
| `hotfix/*` → `develop` | Merge commit | Propagate fix to development |

## Git Flow Lifecycle

```
main:     ─────●───────────────────●────────●──────
               │                   ↑        ↑
release:       │           ●──●───●│   hotfix:●──●
               │           ↑      ││        ↑  │↓
develop:  ─────●───●──●────●──────●●────────●──●──
                   ↑  ↑
feature:       ●──●│  ●──●
               A   │  B
```

### Feature Workflow
1. Create `feature/*` from `develop`
2. Implement with atomic commits (Conventional Commits)
3. Open PR targeting `develop`
4. Squash merge after approval

### Release Workflow
1. Create `release/*` from `develop` when features are complete
2. Only bug fixes, documentation, and release prep in release branch
3. Merge `release/*` into `main` with a merge commit
4. Tag `main` with the version (e.g., `v1.2.0`)
5. Merge `release/*` back into `develop`

### Hotfix Workflow
1. Create `hotfix/*` from `main`
2. Fix the critical issue with minimal changes
3. Merge `hotfix/*` into `main` with a merge commit
4. Tag `main` with the patch version (e.g., `v1.2.1`)
5. Merge `hotfix/*` into `develop` to propagate the fix

## Branch Protection Rules

| Branch | Required | Enforcement |
|--------|----------|-------------|
| `main` | PR required, no direct push | CI must pass, 1+ approval |
| `develop` | PR required, no direct push | CI must pass, 1+ approval |
| `release/*` | PR required for merge to main | CI must pass |
| `feature/*` | No restrictions | — |
| `hotfix/*` | No restrictions | — |

## Forbidden

- Direct commits to `main` — always use PRs
- Direct commits to `develop` — always use PRs
- Force-pushing to `main` or `develop`
- Merging `feature/*` directly into `main` (must go through `develop`)
- Merging `develop` directly into `main` (must go through `release/*`)
- Keeping stale branches after merge (delete after merge)
- Releasing without a `release/*` branch
- Skipping the back-merge of `release/*` or `hotfix/*` into `develop`
