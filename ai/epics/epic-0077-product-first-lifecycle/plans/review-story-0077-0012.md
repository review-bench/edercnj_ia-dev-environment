# Specialist Review — story-0077-0012

**Story:** story-0077-0012 — Skill x-promote-ideation (x-feature-ideate output → persistent)
**Branch:** feat/task-0077-0012-003-promote-ideation-smoke
**Review Date:** 2026-05-05
**Reviewers:** QA, Security

---

## Score Summary

```
+---------------+-------+--------------------+
|    Review     | Score |      Status        |
+---------------+-------+--------------------+
| QA            | 28/36 | Partial            |
| Security      | 22/30 | Partial            |
+---------------+-------+--------------------+
Total: 50/66 (76%)
OVERALL: PARTIAL — merge with remediation
```

---

## Issue Summary

`CRITICAL: 0 | HIGH: 1 | MEDIUM: 2 | LOW: 2`

---

## Critical Findings

_None_

---

## High Findings

| ID | Engineer | File | Description |
|----|----------|------|-------------|
| QA-01 | QA | adapter/inbound/cli/XPromoteIdeationCommand.java | CLI `call()` does not invoke `PromoteIdeationOrchestrationUseCase` — the command prints source/mode but does not execute actual promotion logic. Orchestration is tested in isolation (smoke test) but the CLI adapter is not wired to the use case. |

---

## Medium Findings

| ID | Engineer | File | Description |
|----|----------|------|-------------|
| QA-02 | QA | adapter/inbound/cli/XPromoteIdeationCommand.java:46 | DIP violation: command is not injected with `PromoteIdeationOrchestrationUseCase` via constructor — direct instantiation will be required once wired, which breaks testability. |
| SEC-01 | Security | adapter/inbound/cli/XPromoteIdeationCommand.java | `--from-file` path value is accepted as-is with no normalization or path-traversal rejection. `Path.normalize()` + prefix guard (Rule 06) must be applied before any I/O is introduced. |

---

## Low Findings

| ID | Engineer | File | Description |
|----|----------|------|-------------|
| QA-03 | QA | application/ideation/PromoteIdeationOrchestrationUseCase.java | `promote()` guard `validator == null` in constructor is correct. However, `nextSequence ≤ 0` is not validated; a sequence of 0 would produce `ideation-0000` which may conflict with the template convention. |
| SEC-02 | Security | application/ideation/PromoteIdeationOrchestrationUseCase.java | `failureReason` is assembled via `String.join("; ", validation.errors())` — downstream callers that surface this string in CLI output could expose internal validation messages. Low risk for CLI scope. |

---

## Passed Items

| # | Item | Notes |
|---|------|-------|
| 1 | Ideation-id format regex `ideation-\\d{4}` | Correctly enforces 4-digit zero-padded pattern |
| 2 | Guard `!fromStdin && (fromFile == null)` | Both source flags validated before execution |
| 3 | `IdeationPromotionResult` record | Immutable; success/failure are mutually exclusive factory methods |
| 4 | Smoke test coverage | 8 scenarios covering CLI validation, orchestration round-trips, and domain validator |
| 5 | `IdeationValidator` used via constructor injection in use case | OCP-compliant; test fake can replace |
| 6 | Auto-sequence padding `ideation-%04d` | Correctly zero-pads up to `ideation-9999` |

---

## Recommendations

1. **Priority HIGH:** Wire `XPromoteIdeationCommand` to `PromoteIdeationOrchestrationUseCase` before the command is integrated into the root picocli command tree.
2. **Priority MEDIUM:** Add `--from-file` path normalization + prefix guard (`Path.of(fromFile).toAbsolutePath().normalize()`) per Rule 06.
3. **Priority LOW:** Validate `nextSequence ≥ 1` in `promote()` to prevent `ideation-0000`.
