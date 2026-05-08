# Remediation Tracker — story-0064-0008

**Story:** story-0064-0008 — CHANGELOG seed `[Unreleased] [Breaking]`
**Epic:** EPIC-0064
**Total findings:** 6 | **Fixed:** 1 | **Open (epic-level):** 1 | **Non-blocking (LOW):** 4

## Findings Tracker

| Finding ID | Engineer | Severity | Description | Status | Fix Commit SHA |
| :--- | :--- | :--- | :--- | :--- | :--- |
| FIND-001 | Tech Lead | CRITICAL | LINE 94.34% / BRANCH 88.45% — pre-existing coverage gap on develop — must be addressed at epic integrity gate | ⚠️ Open (epic-level) | — |
| FIND-002 | QA | MEDIUM | QA-8: missing negative test for `isExcludedNamespace()` | ✅ Fixed | `6dfdd5187` |
| FIND-003 | QA | LOW | QA-1: test method names two-segment instead of three-segment | Advisory | — |
| FIND-004 | QA | LOW | QA-4: Windows separator and suffix-only boundary untested | Advisory | — |
| FIND-005 | QA | LOW | QA-7: `isPlanningArtifact` integration-level test missing | Advisory | — |
| FIND-006 | QA | LOW | QA-9: Javadoc/DisplayName reference wrong story ID | Advisory | — |

## Remediation Summary

| Status | Count |
| :--- | :--- |
| Fixed | 1 |
| Open (epic-level action required) | 1 |
| Advisory (non-blocking LOW) | 4 |
| **Total** | **6** |

## Notes

- FIND-001 (coverage): The gap is project-wide and pre-dates EPIC-0064. Confirmed identical to develop baseline. Epic integrity gate (Phase 4) must address before `epic/0064 → develop` PR. Options: (a) dedicated coverage story in EPIC-0064, (b) ADR exception per Rule 05 RULE-005-01 Option 3.
- FIND-002 (QA-8): Fixed in commit `6dfdd5187` — added `isExcludedNamespace_planningArtifactPath_returnsFalse()` asserting false for `plans/epic-0064/plans/story-0064-0001.md`.
