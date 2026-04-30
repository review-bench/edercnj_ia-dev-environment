<!-- template-version: 1.0 -->
---
schema-version: "1.0"
generated-by: x-review@3f48abe31033f5d86194ac476112db42ca911d7f
story-id: story-0067-0004
epic-id: EPIC-0067
date: 2026-04-29T12:00:00Z
decision: GO-WITH-RESERVATIONS
score: 49
score-max: 55
severity-counts:
  critical: 0
  high: 0
  medium: 2
  low: 4
  info: 0
blocking-findings: []
reviewers:
  - qa
  - performance
  - security
  - devops
---

# Specialist Review — story-0067-0004

## Decision

**GO-WITH-RESERVATIONS**

Score: 100/112 (89.3%) — No critical or high findings. Two medium findings (untested exit-3 path, untested path-traversal check). Non-blocking; story may merge with follow-up improvement stories.

## Score Dashboard

| Specialist | Score | Max | Status |
|-----------|-------|-----|--------|
| QA | 30 | 36 | Partial |
| Performance | 24 | 26 | Partial |
| Security | 27 | 30 | Partial |
| DevOps | 19 | 20 | Partial |
| **Total** | **100** | **112** | **GO-WITH-RESERVATIONS** |

## Severity Summary

`CRITICAL: 0 | HIGH: 0 | MEDIUM: 2 | LOW: 4 | INFO: 0`

## Blocking Findings

None.

## Non-Blocking Findings

### MEDIUM

**[QA-16] INVALID_EXEMPTION exit-3 path not tested**
- File: `src/test/java/dev/iadev/skills/AuditReviewFrontmatterTest.java`
- Description: The audit script exits with code 3 when an `audit-exempt` HTML comment marker has no reason text after the colon. Neither `AuditReviewFrontmatterTest` nor `Epic0067ReviewFrontmatterSmokeTest` exercises this branch. Exit-3 is a documented contract in Rule 26 §Standardized and in the script header.
- Fix: Add `@Test void auditScript_auditExemptMissingReason_exitsThree()` that writes a review file with an empty-reason `audit-exempt` marker and asserts `exitCode == 3` + `stderr.contains("INVALID_EXEMPTION")`.

**[SEC-16] Path traversal prevention code has no test coverage**
- File: `src/test/java/dev/iadev/skills/AuditReviewFrontmatterTest.java`
- Description: Lines 261-264 of the script implement a `realpath`-based path traversal guard. No test exercises a file path that resolves outside `REPO_ROOT`. The guard exists but is unverified.
- Fix: Add a test that creates a symlink to a file outside `tempDir` and verifies exit 2 with `OPERATIONAL_ERROR`.

### LOW

**[QA-17] audit-exempt happy path not tested** — File path skipped when a valid `audit-exempt` marker (with non-empty reason text) is present is untested. Add test asserting exit 0 when marker has a reason.

**[QA-18] Unknown flag OPERATIONAL_ERROR not tested** — `run(List.of("--bad-flag"))` should exit 2; not exercised.

**[SEC-15] `is_grandfathered()` uses `grep -q` without `-F`** — story_id is regex-constrained (safe), but `-F` (fixed string) would be more defensive idiom. Low risk.

**[DEVOPS-11] ScriptsAssembler update not directly asserted** — Golden file tests verify file presence indirectly, but no unit assertion in `ScriptsAssemblerTest` verifies `audit-review-frontmatter.sh` appears in the `AUDIT_SCRIPTS` list.

## Passed Highlights

- 13 tests (8 behavioral + 5 smoke) with specific exit code + stderr assertions — no weak assertions
- `@Nested` class organization per scenario — well-structured
- Bash 3.2 compatibility rigorously maintained (`mapfile` → `while read`, `declare -A` → grep pipeline, empty array guards)
- Path traversal prevention implemented (realpath + REPO_ROOT prefix check)
- `--self-check` flag implemented per Rule 26 contract
- All 10 golden profiles updated
- Rule 26 §Catalog-before-Add satisfied (docs/audit-gates-catalog.md created with 18 entries)
- `governance/baselines/review-frontmatter-baseline.txt` created as empty + documented immutability
