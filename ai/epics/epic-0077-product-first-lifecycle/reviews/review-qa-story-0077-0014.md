ENGINEER: QA
STORY: story-0077-0014
SCORE: 20/36
STATUS: Rejected
---
PASSED:
- [QA-01] Domain model completeness (2/2): C4Diagram record enforces all invariants via compact constructor; C4OutputFormat.fromString() handles null → default MERMAID; C4Level enum covers CONTEXT/CONTAINER/COMPONENT/CODE.
- [QA-02] Test naming convention (2/2): All test methods follow `[method]_[scenario]_[expectedBehavior]` — e.g. `planContext_validProductId_returnsContextDiagram`, `fromString_unknownValue_throwsIllegalArgumentException`.
- [QA-03] Domain unit tests present (2/2): ProductC4PlannerTest, CapabilityC4PlannerTest, C4OutputFormatTest, C4CodeLevelValidatorTest cover happy-path, null input, blank input, and unknown format scenarios.
- [QA-04] Renderer unit tests present (2/2): C4ContextRendererTest, C4ContainerRendererTest, C4ComponentRendererTest, C4CodeRendererTest each cover render() level mismatch throws + renderHeader() formatting including HTML escape.
- [QA-05] Smoke test breadth (2/2): XArchPlanC4SmokeTest covers mermaid/plantuml formats for product and capability commands, default format detection, and HTML escaping end-to-end.

FAILED:
- [QA-06] DRY — renderer triplication (0/2) -- adapter/outbound/documentation/C4ContextRenderer.java:1, C4ContainerRenderer.java:1, C4ComponentRenderer.java:1 -- Fix: extract a single C4DiagramRenderer with level-aware dispatch (one `render(C4Diagram diagram)` method delegating on `diagram.level()`). The only variation between the three is the level name string in the error message and the `renderHeader` label. Extracting eliminates ~80 lines of duplicated code. [HIGH]
- [QA-07] DIP — direct instantiation in inbound adapters (0/2) -- adapter/inbound/cli/XArchPlanProductCommand.java:46 (`new ProductC4Planner()`), XArchPlanCapabilityCommand.java:46 (`new CapabilityC4Planner()`) -- Fix: inject planners via constructor (picocli supports constructor injection via `@ParentCommand` or factory pattern). Rule 04: adapters depend on domain ports/interfaces, not concrete domain classes. [MEDIUM]
- [QA-08] Forbidden pattern — String + concatenation in content building (0/2) -- domain/architecture/ProductC4Planner.java:29-44, CapabilityC4Planner.java:31-50 -- Rule 03 §Forbidden: "String concatenation with + in messages or content building". Fix: use Java text blocks (`"""..."""`) for multi-line diagram content construction. [MEDIUM]

PARTIAL:
- [QA-09] CLI command unit tests (1/2) -- adapter/inbound/cli/ -- No XArchPlanProductCommandTest or XArchPlanCapabilityCommandTest exist. Smoke test covers happy path only; EXIT_VALIDATION path (blank productId, invalid format) is untested at unit level. Fix: add command unit tests covering each exit code path. [MEDIUM]
- [QA-10] Generic Exception catch (1/2) -- adapter/inbound/cli/XArchPlanProductCommand.java:51, XArchPlanCapabilityCommand.java:51 -- Catches `Exception` broadly. For CLI commands this is acceptable operationally but masks unexpected runtime errors. Fix: catch only `IllegalArgumentException` (the contract of planners). [LOW]
