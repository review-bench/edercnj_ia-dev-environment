---
requires-capabilities: [governance.bug-lifecycle]
skill-version: "1.0"
---

# Bug PR Lifecycle Wiring

This document describes how bug story PRs integrate with the status lifecycle.

## PR Template

Bug story PRs use `_TEMPLATE-PR-BUG.md`. The template is selected by `x-create-pr`
when the branch follows the pattern `bug/<bug-id>-*`.

```bash
# x-create-pr auto-selects PR-BUG template when story is a bug story
if [[ "$STORY_KIND" =~ (regression-test|fix|doc-update|rollback-plan) ]]; then
  PR_TEMPLATE="_TEMPLATE-PR-BUG.md"
fi
```

## Status Transitions Per Story PR

| Story Kind | PR Opened | PR Merged | Status Before | Status After |
| :--------- | :-------- | :-------- | :------------ | :----------- |
| story-01-regression-test | creates failing test (RED) | — | Pendente | Em Investigação |
| story-02-fix | implements fix (GREEN) | merge | Em Investigação → Em Correção | Concluída |
| story-03-doc-update | updates documentation | merge | no change | no change |
| story-04-rollback-plan | adds rollback instructions | merge | no change | no change |

## Transition Enforcement Rules

1. `story-02-fix` PR MUST NOT be opened before `story-01-regression-test` is merged.
2. The regression test MUST be GREEN (passing) before `story-02-fix` PR is merged.
3. Status `Concluída` is ONLY set after the `story-02-fix` PR merges AND the regression
   test is confirmed GREEN in CI.
4. `Falha` is set if the fix validation fails (regression test RED after merge).
   The bug returns to `Em Correção` for re-investigation.

## Bug PR Body Sections

Every bug PR body contains:

1. **Bug Fix Summary** — bug ID, severity, scope, story kind, change description
2. **Root Cause** — the identified root cause (from Section 6 of bug.md)
3. **Fix Approach** — the approach taken
4. **Status Transition** — before/after table
5. **Review Checklist** — mandatory verification items
6. **Reproduction Verification** — steps that should no longer reproduce after merge
7. **Orchestrator Evidence** — auto-filled by x-create-pr

## Integration with x-create-pr

`x-create-pr` reads the story kind from the task ID to select the PR template:

```bash
# Detect story kind from branch name
STORY_KIND=$(git branch --show-current | grep -oE '(regression-test|fix|doc-update|rollback-plan)')
```

When `STORY_KIND` matches a bug story pattern, the PR body is rendered from
`_TEMPLATE-PR-BUG.md` with placeholders substituted from the story file and bug.md.
