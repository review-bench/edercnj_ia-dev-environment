# Preflight Bypass Vectors Catalog

> **⚠️ PARTIALLY SUPERSEDED (Java/CI removal).** Preflight's Java gates
> (format/tests/coverage via Maven) were removed with the Java build; bypass
> vectors targeting them no longer apply. Non-Java preflight gates remain.
> Kept for historical reference.

**Story:** story-0063-0013 (PreToolUse Hook Coverage Expansion — Bypass Vectors v2)
**Hook:** `.claude/hooks/enforce-preflight-gates-v2.sh`
**Rule Refs:** Rule 24 §Camada 0, Rule 26 §Camada 0, Rule 27 §Zero-Bypass Lifecycle

This document catalogs all intercepted bypass vectors for the preflight gate hooks.
Vectors blocked by v1 (`enforce-preflight-gates.sh`) are listed in §1.
Vectors added by v2 (`enforce-preflight-gates-v2.sh`) are listed in §2.

---

## §1. Vectors — v1 (story-0063-0004)

| Vector | Category | Pattern | Hook |
| :--- | :--- | :--- | :--- |
| `git-push-story` | push | `git push origin feat/story-*` | v1 |
| `git-push-task` | push | `git push origin feat/task-*` | v1 |
| `git-push-epic` | push | `git push origin feat/epic-*` | v1 |
| `gh-pr-create-epic-base` | pr | `gh pr create --base epic/*` | v1 |
| `gh-pr-create-develop-base` | pr | `gh pr create --base develop` | v1 |
| `git-commit-no-verify` | commit | `git commit -n` / `git commit --no-verify` | v1 |
| `gh-pr-merge-admin` | merge | `gh pr merge --admin` | v1 |
| `skill-x-pr-create` | skill | `Skill(skill: "x-create-pr")` | v1 |

---

## §2. Vectors — v2 (story-0063-0013)

### Build Bypass Vectors

| Vector | Pattern | Exit | Alternative |
| :--- | :--- | :--- | :--- |
| `mvn-skipTests` | `mvn .*-DskipTests` | 2 (BLOCKED) | `/x-execute-tests` for scoped test execution |
| `mvn-skipITs` | `mvn .*-DskipITs` | 2 (BLOCKED) | `/x-execute-e2e-tests` for integration tests |
| `mvn-spotlessSkip` | `mvn .*-Dspotless.check.skip=true` | 2 (BLOCKED) | `/x-format-code` before committing |
| `mvn-testSkip` | `mvn .*-Dmaven.test.skip=true` | 2 (BLOCKED) | `/x-execute-tests` for scoped test execution |
| `mvn-noTestsProfile` | `mvn .*-Pno-tests` | 2 (BLOCKED) | Remove `-Pno-tests` and run `/x-execute-tests` |

### Commit Bypass Vectors

| Vector | Pattern | Exit | Alternative |
| :--- | :--- | :--- | :--- |
| `git-amend-pushed` | `git commit --amend` | 2 (BLOCKED) | New commit via `/x-commit-changes` |
| `git-rebase-skip` | `git rebase --skip` | 2 (BLOCKED) | Resolve conflict then `git rebase --continue` |

### Release Bypass Vectors

| Vector | Pattern | Exit | Alternative |
| :--- | :--- | :--- | :--- |
| `git-tag-delete` | `git tag -d vX.Y.Z` | 2 (BLOCKED) | Hotfix PR via `/x-release --hotfix` |
| `git-push-delete-tag` | `git push --delete origin vX.Y.Z` | 2 (BLOCKED) | Hotfix PR via `/x-release --hotfix` |
| `gh-release-delete` | `gh release delete <release>` | 2 (BLOCKED) | Use `/x-release` for lifecycle management |
| `mvn-release-perform` | `mvn release:perform` | 2 (BLOCKED) | Use `/x-release` orchestrator |
| `git-push-force-protected` | `git push --force origin main\|develop\|epic/*` | 2 (BLOCKED) | Rebase and open a PR |

### Merge Bypass Vectors

| Vector | Pattern | Exit | Alternative |
| :--- | :--- | :--- | :--- |
| `gh-pr-merge-rebase-admin` | `gh pr merge --rebase --admin` | 2 (BLOCKED) | Standard PR merge via `/x-merge-pr` |
| `skill-x-pr-merge` | `Skill(skill: "x-merge-pr")` | 2 (BLOCKED) | Use orchestrators that enforce CI-watch |
| `skill-x-pr-merge-train` | `Skill(skill: "x-manage-pr-merge-train")` | 2 (BLOCKED) | Use `/x-implement-epic Phase 5` |
| `gh-pr-close-merged` | `gh pr close` | 0+WARN (phase=warn) | Verify PR status; use standard merge flow |

---

## §3. Override (CLAUDE_RECOVERY_MODE)

`CLAUDE_RECOVERY_MODE=1` is the **only** bypass variable (RULE-004). When set:
- All v2 vector blocks are bypassed.
- A `recovery_mode_used` event is appended to `events.ndjson` with:
  - `vector`: the specific vector that was bypassed
  - `command`: the intercepted command (truncated to 200 chars)
  - `story_id`, `epic_id`: derived from current git branch
- A WARNING is emitted to stderr identifying the vector and bypass.

Usage should remain below 5% of all intercepted calls (story-0063-0017 audits this threshold).

---

## §4. Cross-References

| Rule | Section | Relevance |
| :--- | :--- | :--- |
| Rule 24 | §Camada 0 | Preventive hook layer definition |
| Rule 26 | §Camada 0 | Hook contract (header, exit codes, latency) |
| Rule 27 | §Zero-Bypass Lifecycle | 12 surfaces; CLAUDE_RECOVERY_MODE constraint |
| story-0063-0004 | v1 hook | Base patterns extended by this story |
| story-0063-0017 | Recovery audit | Consumes `vector` field from NDJSON |
