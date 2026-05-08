# Architecture Plan — story-0077-0014

## 1. Context

Story-0077-0014 extends the product-first lifecycle by adding `x-arch-plan-product` and `x-arch-plan-capability` CLI commands that generate C4 architectural diagrams. These diagrams provide immediate visual representations of product and capability architectures.

## 2. Layer Placement

| Component | Layer | Package |
|-----------|-------|---------|
| `XArchPlanProductCommand` | adapter.inbound.cli | picocli command |
| `XArchPlanCapabilityCommand` | adapter.inbound.cli | picocli command |
| `ProductC4Planner` | domain.architecture | pure domain logic |
| `CapabilityC4Planner` | domain.architecture | pure domain logic |
| `C4ContextRenderer` | adapter.outbound.documentation | format rendering |
| `C4ContainerRenderer` | adapter.outbound.documentation | format rendering |
| `C4ComponentRenderer` | adapter.outbound.documentation | format rendering |
| `C4Diagram` (record) | domain.architecture | value object |
| `C4OutputFormat` (enum) | domain.architecture | value object |

## 3. Architecture Decision

**Domain layer owns C4 model structure.** `ProductC4Planner` and `CapabilityC4Planner` produce `C4Diagram` value objects. Renderers in the outbound adapter convert `C4Diagram` to specific text formats (Mermaid, PlantUML). CLI commands wire the two layers.

**C4 levels in scope:**
- Product → C4 Level 1 (Context: system vs. external) + Level 2 (Container: apps/DBs)
- Capability → C4 Level 2 (Container: components) + Level 3 (Component: classes/packages)

## 4. Dependency Direction

```
adapter.inbound.cli → domain.architecture (C4Planner, C4Diagram, C4OutputFormat)
adapter.outbound.documentation → domain.architecture (C4Diagram, C4OutputFormat)
domain.architecture → (no external deps)
```

## 5. Key Invariants

- Domain generates format-agnostic `C4Diagram`; renderers produce format-specific output.
- SVG injection prevented: text content passed through renderers is escaped.
- Mermaid is the default format; plantuml is also supported.
