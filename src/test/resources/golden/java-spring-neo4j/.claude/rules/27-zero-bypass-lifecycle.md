---
requires-capabilities: []
---
# Rule 27 — Zero-Bypass Lifecycle

> **Consolidated by:** EPIC-0078 (Context Budget Optimization). **Authoritative contract:** Rule 19 (Lifecycle Integrity Contract).
> **Full detail — 13 surfaces, enforcement layers, 2 legitimate exceptions, audit exit codes:**
> `Read src/main/resources/targets/claude/knowledge/lifecycle/zero-bypass.md`

## Contract (Non-Negotiable)

Every story MUST be traceable to `x-implement-story`; every task to `x-implement-task`. No PR targeting `epic/*` or `develop` may be merged without all 13 surface evidence artifacts present on disk and referenced in the PR body (Surface 13: dependency policy gate, conditional on `dependencies.policy.enabled=true`).

## Exceptions (2 Only)

1. `--legacy-flow` for `flowVersion=1` epics (Rule 19).
2. Documented `hotfix/*` single-file fix with `## Hotfix Bypass Justification` in PR body.

## Forbidden

- Direct `git commit` + `gh pr create` without invoking the appropriate orchestrator.
- Marking a story `COMPLETE` without the full evidence artifact set.
- Using undocumented bypass env vars (`CLAUDE_SKIP_AUDIT=1`, etc.) — blocked by `enforce-no-bypass-flags.sh`.