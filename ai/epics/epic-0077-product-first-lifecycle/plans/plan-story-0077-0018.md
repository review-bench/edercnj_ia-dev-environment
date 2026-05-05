# Implementation Plan — story-0077-0018

## Tasks

### TASK-0077-0018-001: qa-engineer.md charter rewrite
- Edit `src/main/resources/targets/claude/agents/core/qa-engineer.md`
- Add Section: **AC Measurability** — each AC must have Unit, Target, Measurement method
- Add Section: **Error Catalog** — expected errors with HTTP status, condition, recovery
- Add Section: **SLO Harness** — reference SLOHarness framework and usage
- Add Section: **Success Metrics** — KPIs tracked post-deployment by QA
- Extend 28-Point Checklist with measurability checks (items 29-36)

### TASK-0077-0018-002: ErrorCatalog.yaml + SLOHarness
- Create `src/main/resources/qa/ErrorCatalog.yaml` with canonical error entries
- Create `src/main/java/dev/iadev/infrastructure/qa/SLOHarness.java`:
  - Record `SLOSpec(String id, double target, String windowDescription)`
  - Record `SLOResult(boolean passed, double observedValue, double targetValue, double delta)`
  - Method `SLOResult validate(SLOSpec spec, double observedValue)`
- Create `src/test/java/dev/iadev/infrastructure/qa/SLOHarnessTest.java` with unit tests

### TASK-0077-0018-003: Smoke test
- Create `src/test/java/dev/iadev/infrastructure/qa/QaCharterSmokeTest.java` (Java smoke test)
- Tests: SLOHarness validates above-target SLO → pass, below-target → fail, error catalog YAML readable

## Sequence
001 → 002 → 003
