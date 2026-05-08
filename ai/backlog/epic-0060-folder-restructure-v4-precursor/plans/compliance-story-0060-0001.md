# Compliance Assessment — story-0060-0001: PathResolver helper + schema v4

**Story:** story-0060-0001
**Epic:** EPIC-0060
**Date:** 2026-04-27
**Overall Compliance:** PASS (pending implementation)

---

## 1. Applicable Project Rules

| Rule | Title | Applicability to this story |
| :--- | :--- | :--- |
| Rule 01 | Project Identity | `PathResolver` is infrastructure — no framework annotations in `util/` |
| Rule 03 | Coding Standards | Method ≤ 25 lines, class ≤ 250 lines, no null returns, no `System.out` |
| Rule 04 | Architecture Summary | `util/` must not import `adapter.*`; `domain/model/` must import only stdlib |
| Rule 05 | Quality Gates | ≥ 95% line, ≥ 90% branch — absolute gate applies |
| Rule 06 | Security Baseline | Path normalization, no symlink follow, no path traversal (see security-story-0060-0001.md) |
| Rule 08 | Release Process | Conventional Commits; no CHANGELOG update needed (utility class, no API change) |
| Rule 09 | Branching Model | 3 feature branches, each merges via PR into `epic/0060` |
| Rule 19 | Backward Compatibility | `ExecutionState` field addition must be optional; Rule 19 fallback matrix updated with v4 |

---

## 2. EPIC-0060 Internal Rules

| ID | Title | Compliance Status |
| :--- | :--- | :--- |
| RULE-001 | Backward Compat via flowVersion | PathResolver returns `plans/epic-{id}` when probe is negative — v3 epics unaffected |
| RULE-002 | PathResolver as Single Source of Truth | This story creates the single authority — zero other string-hardcoded paths remain after story merges |
| RULE-011 | Compat Layer Removible | Probe logic is isolated in `probeV4()` package-private method — Story 6 removes it cleanly |

---

## 3. Rule 03 — Coding Standards Compliance

| Constraint | Limit | Expected | Status |
| :--- | :--- | :--- | :--- |
| Method length | ≤ 25 lines | All methods ≤ 15 lines | COMPLIANT |
| Class length | ≤ 250 lines | `PathResolver` ~120 lines, `UnitType` ~25 lines | COMPLIANT |
| Parameters per function | ≤ 4 | Max 3 (`epicId`, `type`, `unitId`) | COMPLIANT |
| Line width | ≤ 120 chars | All lines checked by formatter | COMPLIANT |
| No null returns | Mandatory | Methods return `Path` or throw | COMPLIANT |
| No `System.out` | Forbidden | Structured logging only | COMPLIANT |
| No wildcard imports | Forbidden | `java.nio.file.Path`, `java.nio.file.Files` (explicit) | COMPLIANT |
| Intent-revealing names | Required | `probeV4`, `validateEpicId`, `epicDir` — all descriptive | COMPLIANT |

---

## 4. Rule 04 — Architecture Compliance

| Check | Requirement | Assessment |
| :--- | :--- | :--- |
| `util/PathResolver.java` imports | `java.nio.file.*` only | PASS — zero external library imports |
| `domain/model/ExecutionState.java` imports | Standard library + Jackson annotations | PASS — Jackson is the approved serialization library |
| `util/` ↔ `domain/` | `util/` must not import `domain/` | PASS — PathResolver has no domain imports |
| Dependency direction | `adapter → application → domain ← adapter.outbound` | PASS — PathResolver sits in `util/`, orthogonal to the hexagonal layers |

---

## 5. Rule 05 — Quality Gates

| Metric | Threshold | Enforcement |
| :--- | :--- | :--- |
| Line coverage | ≥ 95% | Absolute gate (pre-existing deficit must be closed in-PR) |
| Branch coverage | ≥ 90% | Absolute gate |
| Test naming | `[method]_[scenario]_[expected]` | Enforced in test plan cycles |
| TDD compliance | RED → GREEN → REFACTOR per cycle | Commit history must show test before implementation |
| Double-Loop TDD | Gherkin → acceptance tests → unit tests | 6 AC scenarios mapped to 14 TPP cycles |

**Note on absolute gate:** This story adds new production code. Coverage must be ≥ 95% line
and ≥ 90% branch for the entire repository after the merge, not just the new files.
If a pre-existing gap exists on `develop`, it must be closed in this PR or a predecessor.

---

## 6. Rule 09 — Branching Model Compliance

| Item | Required | Planned |
| :--- | :--- | :--- |
| Branch per task | Yes | 3 branches (001, 002, 003) |
| Branch naming | `feat/task-{id}-{desc}` | All 3 follow the pattern |
| PR target | `epic/0060` | Correct per Rule 21 (flowVersion "2") |
| Squash merge | Yes | Applied per Rule 09 |

---

## 7. Rule 19 — Backward Compatibility

The `flowVersion` field addition to `ExecutionState` requires a Rule 19 fallback matrix entry.

**New entry to add in `19-backward-compatibility.md` §Fallback Matrix:**

| Condition on field | Resolved value | Behavior | Warning? |
| :--- | :--- | :--- | :--- |
| Field = `"4"` (explicit) | `"4"` | v4 `ai/epics/` layout | No |

This is additive — no existing row is modified.
`PathResolver` probes the filesystem rather than reading `flowVersion` directly,
so pre-existing `"2"` state files continue to resolve to v3 paths (RULE-001).

---

## 8. Compliance Summary

| Area | Status | Notes |
| :--- | :--- | :--- |
| Coding standards (Rule 03) | PASS | All size limits respected |
| Architecture (Rule 04) | PASS | No layer violations |
| Quality gates (Rule 05) | PENDING | Coverage validated at merge time |
| Security baseline (Rule 06) | PASS | See security assessment |
| Branching model (Rule 09) | PASS | 3 task branches targeting `epic/0060` |
| Backward compatibility (Rule 19) | PASS | Additive field; matrix updated |
| EPIC-0060 internal rules | PASS | RULE-001/002/011 all satisfied |
