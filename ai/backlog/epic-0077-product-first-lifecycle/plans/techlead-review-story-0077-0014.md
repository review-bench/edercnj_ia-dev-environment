# Tech Lead Review — story-0077-0014

**Status:** CONDITIONAL — remediation required before merge to epic/0077

## Review Points

### Architecture

- `C4Diagram` record and `C4OutputFormat` enum correctly placed in `domain/architecture/` with zero external dependencies. Domain purity maintained.
- `ProductC4Planner` and `CapabilityC4Planner` correctly placed in domain layer — pure computation, no I/O.
- `C4ContextRenderer`, `C4ContainerRenderer`, `C4ComponentRenderer` correctly placed in `adapter/outbound/documentation/`. However, they are structurally identical — Rule 03 DRY violation (see QA-06). **Blocking: extract single C4DiagramRenderer.**
- `XArchPlanProductCommand` and `XArchPlanCapabilityCommand` directly instantiate domain planners (`new ProductC4Planner()`). Rule 04 DIP violation — inbound adapters must not create domain objects. **Blocking: inject via constructor.**
- `C4CodeRenderer` and `TaskC4CodePlanner` (code-level diagrams) correctly follow the same structural pattern as context/container/component.

### Coding Standards

- String `+` concatenation used in `ProductC4Planner.buildContext()`, `buildContainer()`, `CapabilityC4Planner.buildContainer()`, `buildComponent()` — Rule 03 §Forbidden. **Blocking: replace with Java text blocks.**
- `C4Diagram` compact constructor validates all invariants inline — correct.
- `escape()` helper is private and correctly handles `&`, `<`, `>`, `"` in the right order (ampersand first). Correct.
- Exit codes defined as named constants (`EXIT_SUCCESS=0`, `EXIT_VALIDATION=1`, `EXIT_EXECUTION=2`) — correct pattern.

### Test Quality

- Domain unit tests present: `ProductC4PlannerTest`, `CapabilityC4PlannerTest`, `C4OutputFormatTest`, `C4CodeLevelValidatorTest`.
- Renderer unit tests present for each renderer class.
- `XArchPlanC4SmokeTest` (6 scenarios) covers mermaid/plantuml, default format, HTML injection — adequate smoke coverage.
- **Missing:** `XArchPlanProductCommandTest` and `XArchPlanCapabilityCommandTest` at unit level. EXIT_VALIDATION code paths (blank ID, invalid format string) are not tested in isolation.

### Coverage

Full test suite run passes. Line coverage ≥ 96%, branch coverage ≥ 91% (from verify envelope). Coverage thresholds met.

### Specialist Review Summary

| Specialist | Score | Status |
|---|---|---|
| QA | 20/36 | Rejected |
| Performance | 20/26 | Partial |
| Security | 24/30 | Partial |
| DevOps | 18/20 | Partial |
| **Total** | **82/112 (73%)** | **REJECTED** |

### Blocking Items

1. **[QA-06] DRY — renderer triplication** (HIGH): Extract `C4DiagramRenderer`.
2. **[QA-07] DIP — direct instantiation** (MEDIUM): Inject planners in CLI commands.
3. **[QA-08] String + concatenation** (MEDIUM): Replace with Java text blocks in planners.
4. **[QA-09] Missing CLI command unit tests** (MEDIUM): Add `XArchPlanProductCommandTest`, `XArchPlanCapabilityCommandTest`.

## Verdict

CONDITIONAL — 4 blocking items must be resolved. Re-review required after fixes.
