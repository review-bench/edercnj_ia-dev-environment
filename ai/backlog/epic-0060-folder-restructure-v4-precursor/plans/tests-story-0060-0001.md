# Test Plan — story-0060-0001: PathResolver helper + schema v4

**Story:** story-0060-0001
**Epic:** EPIC-0060
**Date:** 2026-04-27
**Framework:** JUnit 5 + AssertJ | **Test class:** `PathResolverTest.java`

---

## 1. Approach

Double-Loop TDD:

- **Outer loop:** 6 Gherkin acceptance scenarios define the behaviour contract.
- **Inner loop:** 11 TPP-ordered unit test cycles implement behaviour incrementally.

All tests are in `java/src/test/java/dev/iadev/util/PathResolverTest.java`.
`ExecutionState` round-trip is in `dev.iadev.domain.model.ExecutionStateTest`.

---

## 2. Acceptance Scenarios (Outer Loop)

Mapped 1-to-1 from story Section 5.2 Gherkin.

| # | Scenario | Test method |
| :--- | :--- | :--- |
| AC-1 | v3 probe negative → returns `plans/epic-{id}` | `epicDir_v3ProbeNegative_returnsLegacyPath` |
| AC-2 | v4 probe positive → returns `ai/epics/` path | `epicDir_v4ProbePositive_returnsAiEpicsPath` |
| AC-3 | `unitDir` v4 story returns correct nested path | `unitDir_v4Story_returnsCorrectNestedPath` |
| AC-4 | Invalid `epicId` throws `IllegalArgumentException` | `epicDir_invalidEpicId_throwsIllegalArgument` |
| AC-5 | `flowVersion "4"` round-trip in `ExecutionState` | `executionState_defaultFlowVersionIs4` |
| AC-6 | Probe fallback for legacy `flowVersion "2"` epic | `epicDir_legacyEpicNoV4Dir_returnsV3Path` |

---

## 3. TPP Unit Test Cycles (Inner Loop)

Scenarios are ordered from degenerate to complex following the Transformation Priority Premise.

### Phase 1 — TASK-001 cycles

#### Cycle 1 — Degenerate (exception path)

```java
@Test
void epicDir_invalidEpicId_throwsIllegalArgument() {
    PathResolver resolver = new PathResolver(tempDir);
    assertThatIllegalArgumentException()
        .isThrownBy(() -> resolver.epicDir("abc"))
        .withMessageContaining("Invalid epicId format");
}
```

**TPP transform:** `{} → nil` — method that always throws before any real logic.

#### Cycle 2 — Constant (v3 fallback)

```java
@Test
void epicDir_v3ProbeNegative_returnsLegacyPath() {
    PathResolver resolver = new PathResolver(tempDir);
    Path result = resolver.epicDir("0001");
    assertThat(result).isEqualTo(tempDir.resolve("plans/epic-0001"));
}
```

**TPP transform:** `nil → constant` — return fixed v3 path when probe finds nothing.

#### Cycle 3 — Scalar (v4 probe)

```java
@Test
void epicDir_v4ProbePositive_returnsAiEpicsPath(@TempDir Path base) throws IOException {
    Path v4Dir = Files.createDirectories(base.resolve("ai/epics/epic-0060-folder-reorg"));
    PathResolver resolver = new PathResolver(base);
    Path result = resolver.epicDir("0060");
    assertThat(result).isEqualTo(v4Dir);
}
```

**TPP transform:** `constant → scalar` — probe detects existing directory and returns it.

---

### Phase 2 — TASK-002 cycles

#### Cycle 4 — Scalar (unitDir v4)

```java
@Test
void unitDir_v4Story_returnsCorrectNestedPath(@TempDir Path base) throws IOException {
    Files.createDirectories(base.resolve("ai/epics/epic-0060-slug"));
    PathResolver resolver = new PathResolver(base);
    Path result = resolver.unitDir("0060", UnitType.STORY, "0001");
    assertThat(result.toString()).contains("stories");
    assertThat(result.toString()).contains("story-0060-0001");
}
```

#### Cycle 5 — Scalar (planDir)

```java
@Test
void planDir_delegatesToUnitDir(@TempDir Path base) throws IOException {
    Files.createDirectories(base.resolve("ai/epics/epic-0060-slug"));
    PathResolver resolver = new PathResolver(base);
    Path result = resolver.planDir("0060", UnitType.STORY, "0001");
    assertThat(result.getFileName().toString()).isEqualTo("plans");
}
```

#### Cycle 6 — Scalar (reviewDir)

```java
@Test
void reviewDir_delegatesToUnitDir(@TempDir Path base) throws IOException {
    Files.createDirectories(base.resolve("ai/epics/epic-0060-slug"));
    PathResolver resolver = new PathResolver(base);
    Path result = resolver.reviewDir("0060", UnitType.STORY, "0001");
    assertThat(result.getFileName().toString()).isEqualTo("reviews");
}
```

#### Cycle 7 — Scalar (reportDir)

```java
@Test
void reportDir_delegatesToUnitDir(@TempDir Path base) throws IOException {
    Files.createDirectories(base.resolve("ai/epics/epic-0060-slug"));
    PathResolver resolver = new PathResolver(base);
    Path result = resolver.reportDir("0060", UnitType.STORY, "0001");
    assertThat(result.getFileName().toString()).isEqualTo("reports");
}
```

#### Cycle 8 — Scalar (epicTelemetry)

```java
@Test
void epicTelemetry_returnsNdjsonPath() {
    PathResolver resolver = new PathResolver(tempDir);
    Path result = resolver.epicTelemetry("0042");
    assertThat(result.getFileName().toString()).isEqualTo("events.ndjson");
}
```

#### Cycle 9 — Scalar (epicState)

```java
@Test
void epicState_returnsExecutionStateJson() {
    PathResolver resolver = new PathResolver(tempDir);
    Path result = resolver.epicState("0042");
    assertThat(result.getFileName().toString()).isEqualTo("execution-state.json");
}
```

#### Cycle 10 — Constant (releasesDir)

```java
@Test
void releasesDir_returnsAiReleases() {
    PathResolver resolver = new PathResolver(tempDir);
    assertThat(resolver.releasesDir()).isEqualTo(tempDir.resolve("ai/releases"));
}
```

#### Cycle 11 — Constant (runsDir)

```java
@Test
void runsDir_returnsAiRuns() {
    PathResolver resolver = new PathResolver(tempDir);
    assertThat(resolver.runsDir()).isEqualTo(tempDir.resolve("ai/runs"));
}
```

---

### Phase 3 — TASK-003 cycles

#### Cycle 12 — Constant (ExecutionState default flowVersion)

```java
@Test
void executionState_defaultFlowVersionIs4() {
    ExecutionState state = new ExecutionState();
    assertThat(state.getFlowVersion()).isEqualTo("4");
}
```

#### Cycle 13 — Scalar (legacy deserialization)

```java
@Test
void executionState_legacyFlowVersion2_deserializesCorrectly() throws Exception {
    String json = "{\"flowVersion\":\"2\"}";
    ExecutionState state = objectMapper.readValue(json, ExecutionState.class);
    assertThat(state.getFlowVersion()).isEqualTo("2");
}
```

#### Cycle 14 — Boundary (missing field)

```java
@Test
void executionState_missingFlowVersionField_deserializesAsNull() throws Exception {
    String json = "{}";
    ExecutionState state = objectMapper.readValue(json, ExecutionState.class);
    // null is acceptable; orchestrators apply Rule 19 fallback
    assertThat(state.getFlowVersion()).isNull();
}
```

---

## 4. Coverage Targets

| Class | Line | Branch |
| :--- | :--- | :--- |
| `PathResolver` | 100% | ≥ 90% |
| `UnitType` | 100% | 100% |
| `ExecutionState` (flowVersion path) | ≥ 95% | ≥ 90% |

---

## 5. Test Infrastructure

- JUnit 5 `@TempDir` for filesystem fixtures — no real filesystem modification.
- AssertJ for fluent assertions.
- ObjectMapper (`jackson-databind`) for `ExecutionState` round-trip.
- No `Mockito` required — `PathResolver` is a pure value transformer.
