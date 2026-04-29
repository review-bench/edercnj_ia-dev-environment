<!-- template-version: 1.0 -->
---
schema-version: "1.0"
generated-by: x-review@{{COMMIT_SHA}}
story-id: {{STORY_ID}}
epic-id: {{EPIC_ID}}
date: {{ISO_TIMESTAMP}}
decision: {{DECISION}}
score: {{SCORE}}
score-max: 50
severity-counts:
  critical: {{CRITICAL_COUNT}}
  high: {{HIGH_COUNT}}
  medium: {{MEDIUM_COUNT}}
  low: {{LOW_COUNT}}
  info: {{INFO_COUNT}}
blocking-findings:
{{BLOCKING_FINDINGS_YAML}}
reviewers:
{{REVIEWERS_YAML}}
---
# Specialist Review — {{STORY_ID}}

> **Decision:** {{DECISION}} | **Score:** {{SCORE}}/50

> **Story ID:** {{STORY_ID}}
> **Date:** {{DATE}}
> **Reviewer:** {{REVIEWER}}
> **Engineer Type:** {{ENGINEER_TYPE}}
> **Template Version:** 1.0

## Review Scope

{{REVIEW_SCOPE}}

## Score Summary

00/00 | Status: Approved

> Replace `00/00` with actual score (e.g., `42/50`).
> Replace `Approved` with actual status: `Approved` / `Rejected` / `Partial`.
> Parseable via regex: `(\d+)/(\d+)\s*\|\s*Status:\s*(Approved|Rejected|Partial)`

## Passed Items

| # | Item | Notes |
| :--- | :--- | :--- |
| 1 | {{PASSED_ITEM}} | {{NOTES}} |

## Failed Items

| # | File | Line | Severity | Description |
| :--- | :--- | :--- | :--- | :--- |
| 1 | {{FILE}} | {{LINE}} | Critical | {{DESCRIPTION}} |
| 2 | {{FILE}} | {{LINE}} | High | {{DESCRIPTION}} |
| 3 | {{FILE}} | {{LINE}} | Medium | {{DESCRIPTION}} |
| 4 | {{FILE}} | {{LINE}} | Low | {{DESCRIPTION}} |

> Severity levels: `Critical` / `High` / `Medium` / `Low`

## Partial Items

| # | Item | Status | Notes |
| :--- | :--- | :--- | :--- |
| 1 | {{PARTIAL_ITEM}} | {{PARTIAL_STATUS}} | {{NOTES}} |

## Severity Summary

| Severity | Count |
| :--- | :--- |
| Critical | {{CRITICAL_COUNT}} |
| High | {{HIGH_COUNT}} |
| Medium | {{MEDIUM_COUNT}} |
| Low | {{LOW_COUNT}} |
| **Total** | **{{TOTAL_COUNT}}** |

## Recommendations

{{RECOMMENDATIONS}}
