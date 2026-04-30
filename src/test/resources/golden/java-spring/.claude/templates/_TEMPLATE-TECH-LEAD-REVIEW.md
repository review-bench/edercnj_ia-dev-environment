<!-- template-version: 1.0 -->
---
schema-version: "1.0"
generated-by: x-review-pr@{{COMMIT_SHA}}
story-id: {{STORY_ID}}
epic-id: {{EPIC_ID}}
date: {{ISO_TIMESTAMP}}
decision: {{DECISION}}
score: {{SCORE}}
score-max: 55
severity-counts:
  critical: {{CRITICAL_COUNT}}
  high: {{HIGH_COUNT}}
  medium: {{MEDIUM_COUNT}}
  low: {{LOW_COUNT}}
  info: {{INFO_COUNT}}
blocking-findings: {{BLOCKING_FINDINGS_YAML}}
checklist:
  passed: {{CHECKLIST_PASSED}}
  total: 45
  failed-sections: {{FAILED_SECTIONS_YAML}}
---
# Tech Lead Review — {{STORY_ID}}

> **Decision:** {{DECISION}} | **Score:** {{SCORE}}/55

> **Story ID:** {{STORY_ID}}
> **PR:** {{PR_REFERENCE}}
> **Date:** {{DATE}}
> **Score:** 00/55
> **Template Version:** 1.0

## Decision

**{{DECISION}}**

> Decision values: `GO` / `NO-GO`

## Section Scores

| Section | ID | Score | Max Score |
| :--- | :--- | :--- | :--- |
| Clean Code | A | {{SCORE_A}} | 5 |
| SOLID | B | {{SCORE_B}} | 5 |
| Architecture | C | {{SCORE_C}} | 5 |
| Framework Conventions | D | {{SCORE_D}} | 5 |
| Tests | E | {{SCORE_E}} | 5 |
| TDD Process | F | {{SCORE_F}} | 5 |
| Security | G | {{SCORE_G}} | 5 |
| Cross-File Consistency | H | {{SCORE_H}} | 5 |
| API Design | I | {{SCORE_I}} | 5 |
| Events/Messaging | J | {{SCORE_J}} | 5 |
| Documentation | K | {{SCORE_K}} | 5 |

> Total max score: 55.
> Replace `00/55` with actual score. Replace `Approved` with actual status.
> Parseable via regex: `(\d+)/(\d+)\s*\|\s*Status:\s*(Approved|Rejected|Partial)`

00/55 | Status: Approved

## Cross-File Consistency

{{CROSS_FILE_CONSISTENCY}}

## Critical Issues

| # | File | Line | Description | Impact |
| :--- | :--- | :--- | :--- | :--- |
| 1 | {{FILE}} | {{LINE}} | {{DESCRIPTION}} | {{IMPACT}} |

## Medium Issues

| # | File | Line | Description | Recommendation |
| :--- | :--- | :--- | :--- | :--- |
| 1 | {{FILE}} | {{LINE}} | {{DESCRIPTION}} | {{RECOMMENDATION}} |

## Low Issues

| # | File | Line | Description | Suggestion |
| :--- | :--- | :--- | :--- | :--- |
| 1 | {{FILE}} | {{LINE}} | {{DESCRIPTION}} | {{SUGGESTION}} |

## TDD Compliance Assessment

{{TDD_COMPLIANCE_ASSESSMENT}}

## Specialist Review Validation

{{SPECIALIST_REVIEW_VALIDATION}}

## Verdict

{{VERDICT}}
