# Architecture Plan — story-0060-0001: PathResolver helper + schema v4

**Story:** story-0060-0001
**Epic:** EPIC-0060
**Date:** 2026-04-27
**Status:** Draft

---

## 1. Context

This story establishes the single authoritative path-resolution abstraction for the framework.
Forty-two skills currently embed hard-coded path strings (`plans/epic-XXXX/`, `ai/epics/...`).
`PathResolver` replaces all of them with one class that auto-detects layout version via a
filesystem probe and routes accordingly.

---

## 2. Component Overview

```
util/
  PathResolver.java   ← new, probe-based path factory
  UnitType.java       ← new, enum (STORY | BUG | SPIKE | CHORE)

domain/model/
  ExecutionState.java ← existing, add flowVersion "4" field
```

**Dependency direction:** `util/` has zero imports from `domain/` or `application/`.
`ExecutionState` is a plain domain model. Both sit inside
`java/src/main/java/dev/iadev/`.

---

## 3. PathResolver Design

### 3.1 Constructor (DIP)

```java
public PathResolver(Path basePath) { ... }
```

`basePath` is injected — no `System.getProperty("user.dir")` hard-coding.
Tests pass a temp-dir; production callers pass `Path.of(CLAUDE_PROJECT_DIR)`.

### 3.2 Probe Algorithm

```
probeV4(epicId):
  stream = Files.newDirectoryStream(basePath.resolve("ai/epics"), "epic-" + epicId + "-*")
  if stream has entry → return that entry (first match)
  else               → return Optional.empty()
```

- Uses `Files.newDirectoryStream` with a glob — does **not** follow symlinks.
- Result is memoized per `epicId` within the instance lifetime.
- `IOException` from missing `ai/epics/` directory is caught; falls back to v3 silently
  (logged at `DEBUG`).

### 3.3 Public API

| Method | Return | v3 path | v4 path |
| :--- | :--- | :--- | :--- |
| `epicDir(epicId)` | `Path` | `plans/epic-{id}` | `ai/epics/epic-{id}-{slug}` |
| `unitDir(epicId, type, unitId)` | `Path` | n/a (v4 only) | `epicDir/{type.folder}/{type.prefix}{epicId}-{unitId}-{slug}` |
| `planDir(epicId, type, unitId)` | `Path` | delegates to `unitDir` | `unitDir/plans` |
| `reviewDir(epicId, type, unitId)` | `Path` | delegates to `unitDir` | `unitDir/reviews` |
| `reportDir(epicId, type, unitId)` | `Path` | delegates to `unitDir` | `unitDir/reports` |
| `epicTelemetry(epicId)` | `Path` | `epicDir/telemetry/events.ndjson` | same suffix |
| `epicState(epicId)` | `Path` | `epicDir/execution-state.json` | same suffix |
| `releasesDir()` | `Path` | n/a | `ai/releases` |
| `runsDir()` | `Path` | n/a | `ai/runs` |

### 3.4 Input Validation

`epicId` is validated against `^\d{4}$` before any filesystem access.
Violation throws `IllegalArgumentException("Invalid epicId format: must be 4-digit string")`.
Null `UnitType` throws `IllegalArgumentException("UnitType cannot be null")`.

---

## 4. UnitType Enum Design

```java
public enum UnitType {
    STORY("stories", "story-"),
    BUG("bugs",     "bug-"),
    SPIKE("spikes", "spike-"),
    CHORE("chores", "chore-");

    public final String folder;
    public final String prefix;
}
```

Adding a future unit type (e.g., `HOTFIX`) requires only a new enum value — OCP satisfied.

---

## 5. ExecutionState.java Change

Add field:

```java
/** Layout schema version. "4" = v4 ai/epics/ layout. */
private String flowVersion = "4";
```

Backward compatibility: existing JSON with `flowVersion: "2"` deserializes correctly
(Jackson ignores absence of new default). Rule 19 fallback matrix updated with v4 entry.

---

## 6. Layer Constraints

| Layer | PathResolver imports | Allowed? |
| :--- | :--- | :--- |
| `domain/` | PathResolver | No — domain must not import util |
| `application/` | PathResolver | Yes — use case orchestrators may resolve paths |
| `adapter/` | PathResolver | Yes — inbound/outbound adapters may use |
| `util/` (PathResolver itself) | `java.nio.file.*` only | Yes |

---

## 7. Size Constraints (Rule 03)

| Artifact | Limit | Expected |
| :--- | :--- | :--- |
| `PathResolver.java` | ≤ 250 lines | ~120 lines |
| Any single method | ≤ 25 lines | all ≤ 15 lines |
| `UnitType.java` | ≤ 250 lines | ~25 lines |

---

## 8. Mini-ADR: Instance vs. Static PathResolver

**Decision:** Instance class with constructor injection.

**Context:** Static methods would require `PowerMock` or `System.setProperty` hacks
to control `basePath` in tests — added complexity with no benefit.

**Alternative rejected:** Static utility class (`PathUtils.epicDir(id)`).
Rejected because it forces test-only filesystem setup on the real working directory.

**Consequence:** Skills receive `PathResolver` via DI. For runtime (Claude Bash),
the resolver is instantiated once with `$CLAUDE_PROJECT_DIR`.
