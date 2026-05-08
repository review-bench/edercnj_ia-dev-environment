# Story Completion Report — story-0060-0001

**Story:** PathResolver helper + introdução de schema v4
**Status:** Concluída
**Date:** 2026-04-27
**Epic:** EPIC-0060 (Folder Reorganization v4)

---

## Summary

Delivered the foundation utility class `PathResolver` with automatic v3/v4 layout probe via filesystem glob, plus the `UnitType` enum (STORY/BUG/SPIKE/CHORE). Updated Rule 19 fallback matrix to document `flowVersion: "4"` for the v4 layout (`ai/epics/`). Per Rule 14 (Project Scope Guard), no Java domain class was created for execution-state schema versioning — Rule 19 is the source of truth.

This story unblocks story-0060-0002 (`migrate-layout.sh`), story-0060-0004 (42 SKILL updates), and story-0060-0005 (Rules + Hooks + Assemblers).

## Tasks Executed

| Task | Status | PR | Merge SHA |
|------|--------|-----|-----------|
| TASK-0060-0001-001 (PathResolver base + UnitType) | DONE | #726 | f640662c |
| TASK-0060-0001-002 (unit helper tests) | DONE | #727 | 7a0e14dc |
| TASK-0060-0001-003 (Rule 19 update + golden regen) | DONE | #728 | f2a7a02b |
| Coverage gap fix | DONE | #729 | (merged) |

## Quality Metrics

| Metric | Value | Threshold | Status |
|--------|-------|-----------|--------|
| Line coverage (PathResolver) | 100% | ≥ 95% | PASS |
| Branch coverage (PathResolver) | 100% | ≥ 90% | PASS |
| Test count | 17 | ≥ 6 | PASS |
| Full suite | 3982 / 0 fail | green | PASS |
| Method size max | 22 lines | ≤ 25 | PASS |
| Class size | 134 lines | ≤ 250 | PASS |

## Acceptance Criteria

All 6 Gherkin scenarios from story §5.2 are covered by tests:

- ✅ AC-1: v3 layout — epic without v4 dir returns plans/epic-{id}
- ✅ AC-2: v4 layout — epic with ai/epics/epic-{id}-* dir returns v4 path
- ✅ AC-3: unitDir returns nested work/<type> path
- ✅ AC-4: invalid epicId throws IllegalArgumentException
- ✅ AC-5: Rule 19 documents flowVersion=4 for v4 layout
- ✅ AC-6: probe falls back to v3 when ai/epics empty or non-dir match

## Artifacts

**Phase 1 (planning):**
- plans/epic-0060/plans/arch-story-0060-0001.md
- plans/epic-0060/plans/plan-story-0060-0001.md
- plans/epic-0060/plans/tests-story-0060-0001.md
- plans/epic-0060/plans/tasks-story-0060-0001.md
- plans/epic-0060/plans/security-story-0060-0001.md
- plans/epic-0060/plans/compliance-story-0060-0001.md

**Phase 3 (verification):**
- plans/epic-0060/reports/verify-envelope-story-0060-0001.json
- plans/epic-0060/plans/review-story-0060-0001.md
- plans/epic-0060/plans/techlead-review-story-0060-0001.md
- plans/epic-0060/reports/story-completion-report-story-0060-0001.md (this file)

## Production Files Created/Modified

**Created:**
- java/src/main/java/dev/iadev/util/PathResolver.java (134 lines)
- java/src/main/java/dev/iadev/util/UnitType.java (28 lines)
- java/src/test/java/dev/iadev/util/PathResolverTest.java (196 lines, 17 tests)

**Modified:**
- java/src/main/resources/targets/claude/rules/19-backward-compatibility.md (+5 rows)

**Regenerated:**
- 9 golden fixtures (java-spring, java-quarkus, java-spring-clickhouse, etc.)

## Deviations from Story Specification

1. **`ExecutionState.java` not created.** Story §2 listed `java/src/main/java/dev/iadev/domain/model/ExecutionState.java` as an artifact. Tech-lead review approved the deviation: Rule 14 (Project Scope Guard) explicitly forbids "schema versioning for execution state" as a runtime concern. The schema is documented in Rule 19 (the source of truth) and enforced at runtime by orchestrator skills, not by the generator's Java code.

2. **Coverage gap fix in a separate PR (#729).** Initial implementation had 80% branch coverage on `probeV4EpicDir`. Fixed in a follow-up PR rather than amending TASK-002. Acceptable because all PRs are merged before story is marked complete.

## Next Story

`story-0060-0002` (migrate-layout.sh) is now unblocked.
