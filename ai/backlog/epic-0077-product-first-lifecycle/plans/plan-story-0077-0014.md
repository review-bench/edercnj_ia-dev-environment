# Implementation Plan — story-0077-0014

## Tasks

| Task | Description | Branch |
|------|-------------|--------|
| TASK-0077-0014-001 | `XArchPlanProductCommand` + `ProductC4Planner` + `C4Diagram`/`C4OutputFormat` value objects | `feat/task-0077-0014-001-arch-plan-product` |
| TASK-0077-0014-002 | `XArchPlanCapabilityCommand` + `CapabilityC4Planner` | `feat/task-0077-0014-002-arch-plan-capability` |
| TASK-0077-0014-003 | `C4ContextRenderer`, `C4ContainerRenderer`, `C4ComponentRenderer` | `feat/task-0077-0014-003-c4-renderers` |
| TASK-0077-0014-004 | `XArchPlanC4SmokeTest` E2E smoke | `feat/task-0077-0014-004-arch-plan-c4-smoke` |

## Implementation Order

Inner → outer: domain value objects → planners → renderers → CLI → E2E tests.

## Key Design Choices

- `C4Diagram` record holds: `title`, `level` (CONTEXT/CONTAINER/COMPONENT), `format`, `content` (String)
- `C4OutputFormat` enum: `MERMAID` (default), `PLANTUML`
- `ProductC4Planner.planContext(productId)` → C4Diagram; `.planContainer(productId)` → C4Diagram
- `CapabilityC4Planner.planContainer(capabilityId)` → C4Diagram; `.planComponent(capabilityId)` → C4Diagram
- Renderers are static utility classes that take a `C4Diagram` and return formatted String
