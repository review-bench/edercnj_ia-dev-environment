# Test Plan — story-0077-0014

## Unit Tests

| Class | Tests |
|-------|-------|
| `ProductC4PlannerTest` | planContext returns CONTEXT diagram; planContainer returns CONTAINER diagram; null productId throws |
| `CapabilityC4PlannerTest` | planContainer returns CONTAINER diagram; planComponent returns COMPONENT diagram; null capabilityId throws |
| `C4ContextRendererTest` | renders Mermaid C4Context block; renders PlantUML block |
| `C4ContainerRendererTest` | renders Mermaid C4Container block; renders PlantUML block |
| `C4ComponentRendererTest` | renders Mermaid C4Component block; renders PlantUML block |

## Integration / Smoke Tests

| Class | Tests |
|-------|-------|
| `XArchPlanC4SmokeTest` | product command generates 2 files (context + container); capability command generates 2 files (container + component); default format is mermaid; plantuml format produces different content |
