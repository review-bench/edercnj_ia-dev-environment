# Architecture Plan — story-0077-0018

## Story
Charter rewrite qa-engineer.md: AC measurability + error catalog + SLO harness + success metrics.

## Layer Assignment

| Component | Layer | Package |
| :--- | :--- | :--- |
| qa-engineer.md | agents/core (doc) | `src/main/resources/targets/claude/agents/core/` |
| ErrorCatalog.yaml | resources/qa | `src/main/resources/qa/` |
| SLOHarness | infrastructure/qa | `src/main/java/dev/iadev/infrastructure/qa/` |
| SLOHarnessTest | test | `src/test/java/dev/iadev/infrastructure/qa/` |

## Design Decisions

- `SLOHarness` is a pure domain record + logic class: takes `SLOSpec` (id, target, window) and `double observedValue`, returns `SLOResult(passed, delta)`.
- `ErrorCatalog.yaml` is a static resource; parsed by tests via SnakeYAML-free string parsing (standard library only).
- qa-engineer.md rewrite adds 4 sections: AC Measurability, Error Catalog, SLO Harness, Success Metrics.
- No external dependencies introduced.

## File Footprint

write:
- src/main/resources/targets/claude/agents/core/qa-engineer.md
- src/main/resources/qa/ErrorCatalog.yaml
- src/main/java/dev/iadev/infrastructure/qa/SLOHarness.java
- src/test/java/dev/iadev/infrastructure/qa/SLOHarnessTest.java
read: []
regen: []
