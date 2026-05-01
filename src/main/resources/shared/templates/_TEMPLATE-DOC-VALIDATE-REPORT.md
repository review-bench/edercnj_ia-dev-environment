# Documentation Validation Report — {{STORY_ID}}

**Story:** {{STORY_ID}}
**PR:** #{{PR_NUMBER}}
**Date:** {{VALIDATED_AT}}
**Duration:** {{DURATION_SECONDS}}s

## Overall Result

**{{OVERALL_STATUS}}**

> {{OVERALL_SUMMARY}}

## Dimension Results

| Dimension | Status | Reason |
| :--- | :--- | :--- |
| `readme` | {{README_STATUS}} | {{README_REASON}} |
| `api-specs` | {{API_SPECS_STATUS}} | {{API_SPECS_REASON}} |
| `grpc-proto` | {{GRPC_PROTO_STATUS}} | {{GRPC_PROTO_REASON}} |
| `adr` | {{ADR_STATUS}} | {{ADR_REASON}} |
| `skill-docs` | {{SKILL_DOCS_STATUS}} | {{SKILL_DOCS_REASON}} |
| `system-architecture` | {{SYSTEM_ARCH_STATUS}} | {{SYSTEM_ARCH_REASON}} |

> Status values: `OK` | `FAILED` | `SKIPPED` | `WARNING` | `N/A`

## Failures

{{#if FAILURES}}
{{FAILURES_LIST}}
{{else}}
(none)
{{/if}}

## Warnings

{{#if WARNINGS}}
{{WARNINGS_LIST}}
{{else}}
(none)
{{/if}}

## Stack Configuration

- **Active targets:** {{ACTIVE_TARGETS}}
- **Auto-detect mode:** {{AUTO_DETECT}}
- **Freshness window:** {{FRESHNESS_WINDOW_HOURS}}h ({{FRESHNESS_WINDOW_STATUS}})

## Changed Files Analyzed

```
{{CHANGED_FILES_SUMMARY}}
```

> Note: Paths are relative to repository root. No absolute filesystem paths are included in this report.
