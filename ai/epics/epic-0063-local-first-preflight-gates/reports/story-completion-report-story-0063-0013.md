# Story Completion Report — story-0063-0013

**Story:** PreToolUse Hook v2 — 15 Additional Bypass Vectors
**Epic:** EPIC-0063 (Local-First Pre-Flight Gates)
**Date:** 2026-04-28
**Status:** COMPLETED

## Summary

Extended the Camada 0 PreToolUse gate with 15 additional bypass vectors across 5 categories:
build bypass (5 vectors), commit bypass (2 vectors), release bypass (5 vectors),
merge/skill bypass (3 vectors). The v2 companion hook (`enforce-preflight-gates-v2.sh`)
complements v1 without modifying it — additive extension per OCP.

## Deliverables

| Artifact | Path | Status |
| :--- | :--- | :--- |
| v2 hook (source of truth) | `java/src/main/resources/targets/claude/hooks/enforce-preflight-gates-v2.sh` | DONE |
| v2 hook (.claude/hooks) | `.claude/hooks/enforce-preflight-gates-v2.sh` | DONE |
| Test suite | `src/test/shell/enforce_preflight_gates_v2_test.sh` | DONE (15/15 pass) |
| Bypass vectors catalog | `docs/preflight-bypass-vectors.md` | DONE |
| Specialist review | `plans/review-story-story-0063-0013.md` | GO |
| Tech lead review | `plans/techlead-review-story-story-0063-0013.md` | GO |
| Verify envelope | `reports/verify-envelope-story-0063-0013.json` | passed=true |

## TDD Evidence

- RED: 15 tests at exit 127 (hook absent)
- GREEN: 15 tests passing after implementation

## Vectors Implemented (15)

### Build Bypass (5)
- `mvn-skipTests` — blocks `mvn -DskipTests`
- `mvn-skipITs` — blocks `mvn -DskipITs`
- `mvn-spotlessSkip` — blocks `mvn -Dspotless.check.skip=true`
- `mvn-testSkip` — blocks `mvn -Dmaven.test.skip=true`
- `mvn-noTestsProfile` — blocks `mvn -Pno-tests`

### Commit Bypass (2)
- `git-amend-pushed` — blocks `git commit --amend`
- `git-rebase-skip` — blocks `git rebase --skip`

### Release Bypass (5)
- `git-tag-delete` — blocks `git tag -d vX.Y.Z`
- `git-push-delete-tag` — blocks `git push --delete origin vX.Y.Z`
- `gh-release-delete` — blocks `gh release delete`
- `mvn-release-perform` — blocks `mvn release:perform`
- `git-push-force-protected` — blocks force-push to main/develop/epic/*

### Merge Bypass (3, includes Skill vectors)
- `gh-pr-merge-rebase-admin` — blocks `gh pr merge --rebase --admin`
- `skill-x-pr-merge` — blocks `Skill(skill: "x-pr-merge")` direct invocation
- `skill-x-pr-merge-train` — blocks `Skill(skill: "x-pr-merge-train")` direct invocation

### Warn-Mode (1, not counted in 15)
- `gh-pr-close-merged` — warn in PREFLIGHT_PHASE=warn (default), block in block mode

## Quality Gates

- All 15 acceptance criteria covered by tests
- fail-CLOSED verified (RULE-005): matched vectors always exit 2
- CLAUDE_RECOVERY_MODE=1 bypass emits `recovery_mode_used` event with `vector` field (schema v2)
- Exit codes: 0 (allow) and 2 (block) only — Rule 26 Camada 0 compliant
- Latency: < 50ms for non-intercepted calls (regex pre-filter), < 200ms for blocked calls

## Dependencies Satisfied

- story-0063-0017 (recovery audit): `vector` field added to recovery_mode_used event
- story-0063-0016 (rollout WARN→FAIL): PREFLIGHT_PHASE env var hook provided
