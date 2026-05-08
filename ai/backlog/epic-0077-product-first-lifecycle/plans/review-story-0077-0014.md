# Specialist Review — story-0077-0014

**Story:** story-0077-0014 — XArchPlanC4SmokeTest + C4 Diagram Generation (C4ContextRenderer, C4ContainerRenderer, C4ComponentRenderer)
**Branch:** feat/task-0077-0014-004-arch-plan-c4-smoke
**Review Date:** 2026-05-04
**Reviewers:** QA, Performance, Security, DevOps

---

## Score Summary

```
+---------------+-------+--------------------+
|    Review     | Score |      Status        |
+---------------+-------+--------------------+
| QA            | 20/36 | Rejected           |
| Performance   | 20/26 | Partial            |
| Security      | 24/30 | Partial            |
| DevOps        | 18/20 | Partial            |
+---------------+-------+--------------------+
Total: 82/112 (73%)
OVERALL: REJECTED
```

---

## Issue Summary

`CRITICAL: 0 | HIGH: 1 | MEDIUM: 3 | LOW: 4`

ANY item with score < 2 → MUST be fixed before merge. No exceptions.

---

## Critical Findings

_None_

---

## High Findings

| ID | Engineer | File | Description |
|----|----------|------|-------------|
| QA-06 | QA | adapter/outbound/documentation/ | DRY violation: C4ContextRenderer, C4ContainerRenderer, C4ComponentRenderer are structurally identical — differ only in the level name string. Extract single C4DiagramRenderer. |

---

## Medium Findings

| ID | Engineer | File | Description |
|----|----------|------|-------------|
| QA-07 | QA | adapter/inbound/cli/XArchPlanProductCommand.java:46 | DIP violation: `new ProductC4Planner()` direct instantiation in CLI adapter. Inject via constructor. |
| QA-08 | QA | domain/architecture/ProductC4Planner.java:29 | Rule 03 §Forbidden: String `+` concatenation in content building. Use Java text blocks. |
| QA-09 | QA | adapter/inbound/cli/ | No XArchPlanProductCommandTest or XArchPlanCapabilityCommandTest. EXIT_VALIDATION path untested at unit level. |

---

## Low Findings

| ID | Engineer | File | Description |
|----|----------|------|-------------|
| PERF-07 | Performance | adapter/outbound/documentation/ | renderHeader uses String `+` concatenation; consolidating renderers (QA-06) would enable StringBuilder path. |
| SEC-06 | Security | adapter/inbound/cli/XArchPlanProductCommand.java:51 | Generic `catch (Exception e)` — narrow to IllegalArgumentException. |
| SEC-07 | Security | domain/architecture/CapabilityC4Planner.java:42 | Hardcoded PostgreSQL ContainerDb in all capability diagrams regardless of project database profile. |
| DEVOPS-06 | DevOps | domain/architecture/CapabilityC4Planner.java:42 | Same as SEC-07 — operational mislead risk for no-database projects. |

---

## Individual Reports

- [QA Report](../reviews/review-qa-story-0077-0014.md)
- [Performance Report](../reviews/review-performance-story-0077-0014.md)
- [Security Report](../reviews/review-security-story-0077-0014.md)
- [DevOps Report](../reviews/review-devops-story-0077-0014.md)

---

## Remediation Required

The following items MUST be resolved before the story PR can merge to `epic/0077`:

1. **[QA-06] Extract C4DiagramRenderer** — consolidate C4ContextRenderer, C4ContainerRenderer, C4ComponentRenderer into a single level-aware renderer class. Eliminates ~80 lines of duplicated code.
2. **[QA-07] Inject planners via constructor** — remove `new ProductC4Planner()` / `new CapabilityC4Planner()` from `call()`. Use picocli factory or constructor injection.
3. **[QA-08] Replace String + with text blocks** — ProductC4Planner.buildContext(), buildContainer(), CapabilityC4Planner.buildContainer(), buildComponent() all violate Rule 03 §Forbidden.
4. **[QA-09] Add CLI command unit tests** — cover EXIT_VALIDATION (blank productId, invalid format) and EXIT_EXECUTION paths for both XArchPlanProductCommand and XArchPlanCapabilityCommand.

Low-severity items (SEC-06, SEC-07/DEVOPS-06, PERF-07) are recommended but not blocking merge.
