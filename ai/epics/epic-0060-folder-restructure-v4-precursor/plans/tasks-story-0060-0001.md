# Task Breakdown — story-0060-0001: PathResolver helper + schema v4

**Story:** story-0060-0001
**Epic:** EPIC-0060
**Date:** 2026-04-27

---

## 1. Task Graph

```
[TASK-001: PathResolver base + UnitType]
         |
         |─────────────────────────┐
         ↓                         ↓
[TASK-002: Unit helpers]   [TASK-003: ExecutionState v4]
```

TASK-002 and TASK-003 may run in parallel after TASK-001 merges.

---

## 2. TASK-0060-0001-001: PathResolver base + UnitType enum

| Field | Value |
| :--- | :--- |
| **ID** | TASK-0060-0001-001 |
| **Branch** | `feat/task-0060-0001-001-pathresolver-base` |
| **Layer** | Infrastructure (`util/`) |
| **Size** | S |
| **Type** | Unit |
| **Dependencies** | None |

### Description

Create `UnitType.java` enum and `PathResolver.java` with:
- Constructor accepting `Path basePath` (DIP).
- Private `validateEpicId(String)` guard (regex `^\d{4}$`).
- Package-private `probeV4(String) → Optional<Path>` using `Files.newDirectoryStream`.
- Public `epicDir(String epicId) → Path` dispatching probe result.

### Acceptance Criteria

- [ ] `UnitType` enum compiled with `STORY`, `BUG`, `SPIKE`, `CHORE` values and `folder`/`prefix` fields.
- [ ] `PathResolver` instantiated with `basePath`; no static state.
- [ ] `epicDir("abc")` throws `IllegalArgumentException`.
- [ ] `epicDir("0001")` with no v4 dir returns `basePath.resolve("plans/epic-0001")`.
- [ ] `epicDir("0060")` with temp v4 dir returns that directory path.
- [ ] 3 TPP cycles green (Cycles 1-3).

### File Footprint

```
write:
  - java/src/main/java/dev/iadev/util/UnitType.java
  - java/src/main/java/dev/iadev/util/PathResolver.java
  - java/src/test/java/dev/iadev/util/PathResolverTest.java
read:
  - (none — greenfield)
```

---

## 3. TASK-0060-0001-002: Unit helpers

| Field | Value |
| :--- | :--- |
| **ID** | TASK-0060-0001-002 |
| **Branch** | `feat/task-0060-0001-002-pathresolver-unit-helpers` |
| **Layer** | Infrastructure (`util/`) |
| **Size** | S |
| **Type** | Unit |
| **Dependencies** | TASK-0060-0001-001 |

### Description

Extend `PathResolver` with unit-level helpers:
`unitDir`, `planDir`, `reviewDir`, `reportDir`, `epicTelemetry`, `epicState`,
`releasesDir`, `runsDir`. Each method ≤ 15 lines; delegates to `epicDir` internally.

### Acceptance Criteria

- [ ] `unitDir("0060", STORY, "0001")` returns path containing `stories/story-0060-0001`.
- [ ] `planDir`, `reviewDir`, `reportDir` return `unitDir` + `/plans`, `/reviews`, `/reports`.
- [ ] `epicTelemetry("0060")` returns path ending in `telemetry/events.ndjson`.
- [ ] `epicState("0060")` returns path ending in `execution-state.json`.
- [ ] `releasesDir()` returns `basePath.resolve("ai/releases")`.
- [ ] `runsDir()` returns `basePath.resolve("ai/runs")`.
- [ ] TPP cycles 4-11 green.
- [ ] `PathResolver` total ≤ 150 lines.

### File Footprint

```
write:
  - java/src/main/java/dev/iadev/util/PathResolver.java   (edit)
  - java/src/test/java/dev/iadev/util/PathResolverTest.java (edit)
read:
  - java/src/main/java/dev/iadev/util/UnitType.java
```

---

## 4. TASK-0060-0001-003: ExecutionState flowVersion "4" + Rule 19

| Field | Value |
| :--- | :--- |
| **ID** | TASK-0060-0001-003 |
| **Branch** | `feat/task-0060-0001-003-executionstate-v4` |
| **Layer** | Infrastructure (`domain/model/`) |
| **Size** | S |
| **Type** | Unit |
| **Dependencies** | TASK-0060-0001-001 |

### Description

Add `flowVersion` field to `ExecutionState.java` defaulting to `"4"`.
Existing JSON with `"2"` must deserialize without error (Rule 19 backward compat).
Update `19-backward-compatibility.md` fallback matrix with a `"4"` row.

### Acceptance Criteria

- [ ] `new ExecutionState().getFlowVersion()` returns `"4"`.
- [ ] Jackson deserialization of `{"flowVersion":"2"}` yields `"2"`.
- [ ] Jackson deserialization of `{}` yields `null` (no forced override).
- [ ] `19-backward-compatibility.md` fallback matrix includes `"4"` row.
- [ ] TPP cycles 12-14 green.

### File Footprint

```
write:
  - java/src/main/java/dev/iadev/domain/model/ExecutionState.java (edit)
  - java/src/test/resources/targets/claude/rules/19-backward-compatibility.md (edit)
read:
  - java/src/main/java/dev/iadev/util/PathResolver.java
regen:
  - java/src/main/java/dev/iadev/domain/model/ExecutionState.java
```

---

## 5. Parallelism Evaluation (EPIC-0041)

| Pair | Shared files | Category | Recommendation |
| :--- | :--- | :--- | :--- |
| TASK-001 ‖ TASK-002 | `PathResolver.java`, `PathResolverTest.java` | Hard | Serial — TASK-001 first |
| TASK-001 ‖ TASK-003 | `ExecutionState.java` (regen) | Soft | Parallel safe |
| TASK-002 ‖ TASK-003 | None | None | Parallel safe |
