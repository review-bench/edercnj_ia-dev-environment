# Specialist Review — story-0063-0013

**Story:** PreToolUse Hook v2 — 15 Additional Bypass Vectors
**Epic:** EPIC-0063 (Local-First Pre-Flight Gates)
**Reviewer:** Specialist Review (QA + Security)
**Date:** 2026-04-28
**Verdict:** GO

## QA Review

### Test Coverage
- 15 shell tests (T1–T15) covering all intercept vectors added by v2
- RED → GREEN TDD cycle verified: all 15 tests failed (exit 127) before implementation, all pass after
- Tests exercise each of the 5 vector categories: build, commit, release, merge (Bash), merge (Skill)
- T11 validates the critical allow-path: `git push --force` to non-protected branch exits 0

### Test Quality
- Tests are isolated via mktemp TMP_DIR with `trap 'rm -rf "$TMP_DIR"' EXIT`
- CLAUDE_RECOVERY_MODE is explicitly unset in blocking tests via `CLAUDE_RECOVERY_MODE=""`
- T3/T15 cover recovery mode bypass: exit 0 and stderr WARNING, respectively
- assert_exit and assert_output_contains helpers are consistent with project pattern

### Acceptance Criteria Coverage
- AC: mvn -DskipTests blocked → T6 covers mvn-skipTests vector
- AC: git commit --amend blocked → T7 covers git-amend-pushed vector
- AC: git rebase --skip blocked → T8 covers git-rebase-skip vector
- AC: mvn release:perform blocked → T9 covers mvn-release-perform vector
- AC: gh release delete blocked → T10 covers gh-release-delete vector
- AC: git push --force to main blocked → T5 covers git-push-force-protected vector
- AC: git push --force to feat/ allowed → T11 validates non-protected branch allow-path
- AC: git tag -d vX.Y.Z blocked → T12 covers git-tag-delete vector
- AC: mvn -Dspotless.check.skip blocked → T13 covers mvn-spotlessSkip vector
- AC: CLAUDE_RECOVERY_MODE=1 bypasses with audit → T3, T15
- AC: non-matched call exits 0 → T14

### Gherkin Coverage (§5.2)
- "bloqueia mvn -DskipTests" → T6: PASS
- "bloqueia git push --force em main" → T5: PASS
- "permite git push --force em branch feat/" → T11: PASS
- "recovery mode permite mvn -DskipTests" → T3+T15: PASS
- "bloqueia gh release delete" → T10: PASS

## Security Review

### Input Handling
- All inputs read via `jq -r '.tool_input.command // empty'` — no shell injection risk
- `CMD` variable processed only through `grep -qE` — no eval, no unsafe expansion
- Regex patterns use `\s|$` terminators to prevent partial matches (e.g., `-DskipTests` won't match `-DskipTestsSomething`)

### Fail-CLOSED Verification (RULE-005)
- Malformed JSON: delegated to v1 hook (companion contract, v2 exits 0 on malformed to avoid double-blocking)
- Matched vectors with no CLAUDE_RECOVERY_MODE: always exit 2 — fail-CLOSED confirmed

### Recovery Mode Audit Trail
- `emit_recovery_event` appends JSON with `vector` field to NDJSON — schema v2 (additive over v1 schema)
- Command truncated to 200 chars — no unbounded log growth
- Telemetry write errors swallowed (|| true) — hook never aborts due to telemetry failure

### Protected Branch Detection
- Force-push to main/develop/epic/* is blocked
- Force-push to feat/, fix/, hotfix/ is explicitly allowed (non-protected per Rule 09)
- Detection uses grep pattern matching branch name in command string — conservative, no false negatives on standard usage

### gh-pr-close Warn-Mode
- In PREFLIGHT_PHASE=warn (default), gh pr close exits 0 with WARNING — correct rollout approach
- In PREFLIGHT_PHASE=block, it exits 2 — configurable without code change

## Exit Code Compliance (Rule 26 §Camada 0)
- Exit 0: OK (allow) — correct for non-matched and recovery mode calls
- Exit 2: BLOCKED — all intercept paths exit 2 via `block_with_vector()`
- No exit codes outside 0/2 — compliant with Rule 26 Camada 0 contract

## Verdict

**GO** — 15 vectors implemented, 15 tests passing, fail-CLOSED confirmed, recovery mode audited, force-push protected-branch logic correct, portability maintained.
