---
generated-by: x-test-plan@0792be069d39537b5f4c7c76d7e68372b585697f
generated-at: 2026-04-27T16:50:49Z
story-id: story-0059-0002
---

# Test Plan — story-0059-0002: Origin Markers in Artifacts + Anti-Backfill Audit

## Outer Loop — Acceptance Tests

| # | Scenario | Test Type | TPP Order | Expected Result |
| :--- | :--- | :--- | :--- | :--- |
| AT-01 | Artifact without frontmatter fails audit | Smoke | 1 | exit 1, message contains "missing generated-by frontmatter" |
| AT-02 | Artifact with valid frontmatter + real SHA passes | Smoke | 2 | exit 0 |
| AT-03 | Artifact with fictitious 40-char SHA fails | Smoke | 3 | exit 1, message contains "SHA not found in git history" |
| AT-04 | Artifact committed after story merge fails (backfill detection) | Smoke | 4 | exit 1, message contains "EIE_BACKFILL_DETECTED" |
| AT-05 | Artifact with valid exemption `<!-- audit-exempt: backfill <url> -->` passes | Smoke | 5 | exit 0, output contains "exemption accepted" |
| AT-06 | Artifact with empty exemption `<!-- audit-exempt: backfill -->` fails | Smoke | 6 | exit 3, `EIE_INVALID_EXEMPTION` |
| AT-07 | x-arch-plan SKILL.md contains frontmatter emission instruction | Unit | 7 | grep succeeds for "generated-by" in x-arch-plan SKILL.md |
| AT-08 | x-test-plan SKILL.md contains frontmatter emission instruction | Unit | 8 | grep succeeds for "generated-by" in x-test-plan SKILL.md |

## Inner Loop — Unit Tests (Transformation Priority Premise order)

### `check_frontmatter_origin` function

| # | Test Case | Input | Expected |
| :--- | :--- | :--- | :--- |
| UT-01 | Empty file | empty artifact | return 1 |
| UT-02 | File with no frontmatter block | Content without `---` | return 1, "missing generated-by" |
| UT-03 | Frontmatter block missing `generated-by` | `---\ngenerated-at: 2026-01-01\n---` | return 1, "generated-by field absent" |
| UT-04 | `generated-by` malformed (no `@`) | `generated-by: x-arch-plan` | return 1, "format invalid" |
| UT-05 | `generated-by` with SHA < 40 chars | `generated-by: x-arch-plan@abc123` | return 1, "format invalid" |
| UT-06 | `generated-by` with non-hex chars | `generated-by: x-arch-plan@xyz...(40 chars)` | return 1, "format invalid" |
| UT-07 | `generated-by` with valid format but SHA not in git | `generated-by: x-arch-plan@0000000000000000000000000000000000000000` | return 1, "SHA not found" |
| UT-08 | `generated-by` with valid format and real SHA | actual HEAD SHA | return 0 |

### `check_anti_backfill` function

| # | Test Case | Setup | Expected |
| :--- | :--- | :--- | :--- |
| AB-01 | Artifact not yet in git history | untracked file | return 0 (skip, fail-open) |
| AB-02 | Cannot determine story merge time | story has no merge commit | return 0 (fail-open) |
| AB-03 | Artifact committed before story merge | artifact_ts < merge_ts | return 0 |
| AB-04 | Artifact committed at same time as merge | artifact_ts == merge_ts | return 0 (not strictly after) |
| AB-05 | Artifact committed after story merge | artifact_ts > merge_ts | return 1, "EIE_BACKFILL_DETECTED" |

### Frontmatter emission in SKILL.md files

| # | Test Case | Check Method | Expected |
| :--- | :--- | :--- | :--- |
| FM-01 | x-arch-plan SKILL.md has `generated-by` emission instruction | `grep -c "generated-by"` | ≥ 1 match |
| FM-02 | x-internal-story-build-plan SKILL.md has emission instruction | `grep -c "generated-by"` | ≥ 1 match |
| FM-03 | x-test-plan SKILL.md has emission instruction | `grep -c "generated-by"` | ≥ 1 match |
| FM-04 | x-story-implement SKILL.md has emission for Phase 1E/1F | `grep -c "generated-by"` | ≥ 2 matches |
| FM-05 | x-task-plan SKILL.md has emission instruction | `grep -c "generated-by"` | ≥ 1 match |

## Smoke Test Script Location

`src/test/bash/audit-anti-backfill-smoke.sh`

## Coverage Targets

| Component | Line Coverage Target | Branch Coverage Target |
| :--- | :--- | :--- |
| `check_frontmatter_origin()` | ≥ 95% | ≥ 90% |
| `check_anti_backfill()` | ≥ 95% | ≥ 90% |
| Integration (audit + frontmatter check) | ≥ 95% | ≥ 90% |

Note: Coverage for Bash scripts is measured via smoke test scenario coverage (all 8 acceptance test scenarios covered = 100% scenario coverage).

## TDD Cycle Order (TPP)

1. **Red:** Write AT-01 — empty artifact fails → fails because `check_frontmatter_origin` doesn't exist
2. **Green:** Add `check_frontmatter_origin()` skeleton returning 1 for missing frontmatter
3. **Red:** Write UT-04/UT-05/UT-06 — format validation
4. **Green:** Add regex validation `^[a-z-]+@[0-9a-f]{40}$`
5. **Red:** Write UT-07 — SHA not in git
6. **Green:** Add `git cat-file -t` check
7. **Red:** Write AB-05 — backfill detection
8. **Green:** Add `check_anti_backfill()` with timestamp comparison
9. **Red:** Write AT-05/AT-06 — exemption handling
10. **Green:** Extend `has_audit_exempt()` to handle backfill-specific exemption format
11. **Refactor:** Extract helper `validate_frontmatter_regex()`, clean up error messages
