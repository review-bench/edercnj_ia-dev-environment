# Implementation Plan — story-0077-0015

## Tasks

| Task | Description | Branch |
| :--- | :--- | :--- |
| TASK-0077-0015-001 | C4CodeLevelValidator + TaskC4CodePlanner + C4Level.CODE | feat/task-0077-0015-001-c4-code-validator |
| TASK-0077-0015-002 | C4CodeRenderer (adapter.outbound) | feat/task-0077-0015-002-c4-code-renderer |
| TASK-0077-0015-003 | TaskC4CodeSmokeTest (6 E2E scenarios) | feat/task-0077-0015-003-c4-code-smoke |

## Implementation Order

1. Extend `C4Diagram.C4Level` with `CODE`
2. `C4CodeLevelValidator` with `CodeEntry`, `Dependency`, `ValidationResult` nested records
3. `TaskC4CodePlanner` with Mermaid + PlantUML output
4. `C4CodeRenderer` in adapter.outbound
5. Smoke tests covering all paths
