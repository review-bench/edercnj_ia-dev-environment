# Architecture Plan — story-0077-0015

## Decision

C4 Code level (L4) classes live in `domain/architecture/` — zero external deps. `C4Diagram.C4Level` gains `CODE` variant. `TaskC4CodePlanner` generates the diagram. `C4CodeLevelValidator` enforces hexagonal dependency rules. `C4CodeRenderer` (adapter.outbound) renders the diagram.

## Dependency Direction

```
adapter.inbound → application → domain ← adapter.outbound
```

| Layer | Classes |
| :--- | :--- |
| domain.architecture | C4CodeLevelValidator, TaskC4CodePlanner (+ CODE on C4Level enum) |
| adapter.outbound.documentation | C4CodeRenderer |

## Key Decisions

- `CODE` added to existing `C4Diagram.C4Level` enum — minimal change, backward-compatible
- `C4CodeLevelValidator` is a pure function record validator (no state) — returns `ValidationResult`
- Hexagonal rule: DOMAIN → nothing (outward forbidden); APPLICATION → not adapters; ADAPTER_* → anything
- Mermaid output: `classDiagram` with `namespace` blocks per layer
- PlantUML output: `@startuml` with `package` blocks per layer
