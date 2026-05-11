---
name: x-push-branch
description: "Git workflow: branch, atomic Conventional Commits, push, and PR creation."
user-invocable: true
allowed-tools: Bash, Read
argument-hint: "[branch-name or commit-message]"
requires-capabilities: []
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

# Skill: Commit and Push (slim — ADR-0012)

## Purpose

Standardizes the Git workflow for {{PROJECT_NAME}}. Every feature starts with a branch and ends with a clean commit history following Conventional Commits.

## Triggers

- `/x-push-branch` — commit and push current changes
- `/x-push-branch branch-name` — create branch, commit, and push
- `/x-push-branch "commit message"` — commit with message and push

## Workflow Overview

```text
1. BRANCH   -> Create or verify feature branch (story/task/parent/hotfix patterns)
2. COMMIT   -> Stage and commit using Conventional Commits + optional [TDD*] suffixes
3. PUSH     -> Push to remote with -u tracking
4. PR       -> Create PR (task-PR → parent story branch; story-PR → develop)
```

Detailed step-by-step (branch naming validation, commit format/scopes/rules, hotfix workflow, PR templates, tagging, TDD commit rules, git history storytelling) lives in [`references/full-protocol.md`](references/full-protocol.md):

- **Step 1** (§Step 1 Branch Strategy): full branch hierarchy (main / develop / feat-story / feat-task / hotfix / release); naming patterns for task-branch (`feat/task-XXXX-YYYY-NNN-desc`, max 60 chars), parent-branch (`feat/story-XXXX-YYYY-desc`), legacy story-branch (`feat/story-XXXX-YYYY-kebab`); validation rule table (epic/story/task IDs, charset `[a-z0-9/-]`, length cap); `git checkout`/`git pull`/`git checkout -b` commands per pattern.
- **Step 2** (§Step 2 Commit Convention): Conventional Commits format (`<type>(<scope>): <subject>` + body + footer); 8 types catalog (`feat`/`test`/`fix`/`refactor`/`docs`/`build`/`chore`/`infra`); task-centric scope (`TASK-XXXX-YYYY-NNN`) vs module-based scope; 4 atomic-commit rules (one logical change, 72-char subject, "why" body, tests with features).
- **Step 3** (§Step 3 Workflow Per Story): starting (checkout develop + pull + branch), during-implementation (`git add` paired files + commit with `[TDD]`), finishing (build + `git log develop..HEAD` + `git diff develop...HEAD --stat` + push).
- **Step 4** (§Step 4 Hotfix Workflow): hotfix branches from `main`; PR targets `main`; back-merge PR to `develop` after merge.
- **Step 5** (§Step 5 Pull Request): task-PR template (Summary + Story/Epic refs + Task Plan link + Changed Files table + TDD Summary + Checklist) vs story-PR template (Summary + Test plan); PR title conventions (task: `<type>(TASK-XXXX-YYYY-NNN): desc`; story: `feat(scope): implement story-XXXX-YYYY -- title`); useful gh commands.
- **Step 6** (§Step 6 Tagging Releases): `git tag -a vX.Y.Z -m "..."` + `git push origin vX.Y.Z`.
- **Step 7** (§Step 7 TDD Commit Format): 4 TDD suffix variants (`[TDD]` recommended default, `[TDD:RED]`/`[TDD:GREEN]`/`[TDD:REFACTOR]` fine-grained); additive nature (do not replace Conventional Commits type).
- **Step 8** (§Step 8 Atomic TDD Commit Rules): 5 invariants (default combined commit, test+impl in same commit, separable refactor, one behavior per commit, ~50 line cap).
- **Step 9** (§Step 9 Git History Storytelling): TDD progression order (acceptance + infra → incremental TPP units → refactor/polish).

## Error Handling

| Scenario | Action |
|----------|--------|
| Uncommitted changes when creating branch | Stash or commit before switching |
| Push rejected (remote ahead) | Pull with rebase, then push again |
| Merge conflict during rebase | Report conflict files, suggest resolution |
| PR creation fails (no remote branch) | Push branch first, then retry PR creation |
| Build fails before push | Abort push, report build errors |

## Integration Notes

| Skill | Relationship | Context |
|-------|-------------|---------|
| `x-implement-story` | called-by | Phase 0 (branch from develop) and Phase 5 (push + PR to develop) |
| `x-implement-task` | called-by | Atomic TDD commits during implementation |
| `x-release` | called-by | Release commit and tag creation |
| `x-commit-changes` | delegates-to | Commit creation with task ID scope and TDD tags |
| `x-create-pr` | delegates-to | PR creation with task references and body template |

- Hotfix workflow branches from `main` and creates PRs targeting `main`, then back-merges to `develop`.
- Can be used standalone for any git workflow task.
- For task branches, commit scope MUST use `TASK-XXXX-YYYY-NNN` format.
- For non-task branches, commit scope should match the project's package/module structure.
- Task PRs target the parent story branch; story PRs target `develop`.

## Full Protocol

Minimum viable contract above. Detailed branch naming validation rules, all 9 Conventional-Commits step procedures, hotfix end-to-end flow, PR body templates (task + story + hotfix), and TDD commit semantics live in [`references/full-protocol.md`](references/full-protocol.md) per ADR-0012 (skill body slim-by-default).
