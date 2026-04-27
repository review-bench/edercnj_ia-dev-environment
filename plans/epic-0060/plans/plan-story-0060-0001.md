# Implementation Plan — story-0060-0001: PathResolver helper + schema v4

**Story:** story-0060-0001
**Epic:** EPIC-0060
**Date:** 2026-04-27
**Approach:** Double-Loop TDD (Red → Green → Refactor per task)

---

## 1. Task Sequence

```
TASK-001 ──→ TASK-002
         ──→ TASK-003
```

TASK-001 is the prerequisite for both TASK-002 and TASK-003.
TASK-002 and TASK-003 are independent and may execute in parallel after TASK-001.

---

## 2. TASK-0060-0001-001: PathResolver base + UnitType + epicDir + probe

**Branch:** `feat/task-0060-0001-001-pathresolver-base`
**Size:** S | **Layer:** Infrastructure (`util/`)

### Files Produced

| Action | File |
| :--- | :--- |
| CREATE | `java/src/main/java/dev/iadev/util/UnitType.java` |
| CREATE | `java/src/main/java/dev/iadev/util/PathResolver.java` |
| CREATE | `java/src/test/java/dev/iadev/util/PathResolverTest.java` |

### TDD Cycles (TPP order)

| Cycle | RED test | GREEN implementation |
| :--- | :--- | :--- |
| 1 — Degenerate | `epicDir_invalidEpicId_throwsIllegalArgument` | Regex guard `^\d{4}$` |
| 2 — Constant | `epicDir_v3ProbeNegative_returnsLegacyPath` | Probe returning empty → `plans/epic-{id}` |
| 3 — Scalar | `epicDir_v4ProbePositive_returnsAiEpicsPath` | Probe with temp dir glob match → v4 path |

### Refactor After Green

- Extract `validateEpicId(String)` private method.
- Extract `probeV4(String) → Optional<Path>` package-private method.
- Memoize probe result in `Map<String, Optional<Path>>`.

### Exit Criteria

- [ ] `PathResolverTest` cycles 1-3 green
- [ ] `UnitType` enum compiled with 4 values
- [ ] `PathResolver` ≤ 120 lines
- [ ] No `System.getProperty` call inside `PathResolver`

---

## 3. TASK-0060-0001-002: Unit helpers (unitDir / planDir / reviewDir / reportDir / epicTelemetry / epicState / releasesDir / runsDir)

**Branch:** `feat/task-0060-0001-002-pathresolver-unit-helpers`
**Size:** S | **Layer:** Infrastructure (`util/`)
**Depends on:** TASK-001 merged

### Files Modified

| Action | File |
| :--- | :--- |
| EDIT | `java/src/main/java/dev/iadev/util/PathResolver.java` |
| EDIT | `java/src/test/java/dev/iadev/util/PathResolverTest.java` |

### TDD Cycles (TPP order)

| Cycle | RED test | GREEN implementation |
| :--- | :--- | :--- |
| 4 — Scalar | `unitDir_v4Story_returnsCorrectNestedPath` | `epicDir + work/stories/story-{epic}-{unit}-{slug}` |
| 5 — Scalar | `planDir_delegatesToUnitDir` | `unitDir + /plans` |
| 6 — Scalar | `reviewDir_delegatesToUnitDir` | `unitDir + /reviews` |
| 7 — Scalar | `reportDir_delegatesToUnitDir` | `unitDir + /reports` |
| 8 — Scalar | `epicTelemetry_returnsNdjsonPath` | `epicDir + telemetry/events.ndjson` |
| 9 — Scalar | `epicState_returnsJsonPath` | `epicDir + execution-state.json` |
| 10 — Constant | `releasesDir_returnsAiReleases` | `basePath.resolve("ai/releases")` |
| 11 — Constant | `runsDir_returnsAiRuns` | `basePath.resolve("ai/runs")` |

### Refactor After Green

- Ensure each method ≤ 15 lines.
- Extract shared `subDir(Path base, String... segments)` private helper if 2+ methods share
  the pattern `base.resolve(a).resolve(b)`.

### Exit Criteria

- [ ] All 11 cycles green
- [ ] `PathResolver` still ≤ 150 lines total
- [ ] 100% line coverage on `PathResolver`

---

## 4. TASK-0060-0001-003: ExecutionState flowVersion "4" + Rule 19 update

**Branch:** `feat/task-0060-0001-003-executionstate-v4`
**Size:** S | **Layer:** Infrastructure (`domain/model/`)
**Depends on:** TASK-001 merged

### Files Modified

| Action | File |
| :--- | :--- |
| EDIT | `java/src/main/java/dev/iadev/domain/model/ExecutionState.java` |
| EDIT | `java/src/test/resources/targets/claude/rules/19-backward-compatibility.md` |

### TDD Cycles (TPP order)

| Cycle | RED test | GREEN implementation |
| :--- | :--- | :--- |
| 1 — Constant | `executionState_defaultFlowVersionIs4` | Add `flowVersion = "4"` field with getter |
| 2 — Scalar | `executionState_legacyFlowVersion2_deserializesCorrectly` | Jackson deserialization round-trip |
| 3 — Boundary | `executionState_missingFlowVersionField_deserializesAsNull` | `@JsonProperty` with `defaultValue` |

### Rule 19 Update

Add entry to the Fallback Matrix in `19-backward-compatibility.md`:

| Field value | Resolved | Behavior | Warning? |
| `"4"` | `"4"` | v4 `ai/epics/` layout | No |

### Exit Criteria

- [ ] All 3 cycles green
- [ ] Rule 19 updated with v4 row
- [ ] Backward compat: deserialization of `flowVersion: "2"` still passes

---

## 5. Global DoD Checklist

- [ ] Line coverage ≥ 95% (Rule 05 absolute gate)
- [ ] Branch coverage ≥ 90%
- [ ] Zero compiler warnings
- [ ] All methods ≤ 25 lines (Rule 03)
- [ ] No `System.out` in production code (Rule 03)
- [ ] Test names follow `[method]_[scenario]_[expected]` convention (Rule 05)
- [ ] TDD commits: RED commit precedes GREEN commit in git log
