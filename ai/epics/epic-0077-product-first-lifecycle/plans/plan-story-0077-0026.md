# Implementation Plan — story-0077-0026

**Story:** Refator x-arch-plan integrado com Feature como input
**Status:** Concluída
**Planned at:** 2026-05-05T19:15:00Z

## 1. Scope

Refactor the architecture planning pipeline to accept Feature, Capability, and Product
as first-class inputs, generating mandatory C4 diagrams at each level:
- `XArchPlanFeatureCommand` — generates C4 diagrams from Feature artifact
- `XArchPlanCapabilityCommand` — generates C4 diagrams from Capability artifact
- `XArchPlanProductCommand` — generates C4 diagrams from Product artifact
- `ArchitectureRefactoringUseCase` — orchestrates C4 diagram generation
- Domain models: `FeatureC4Model`, `CapabilityC4Model`, `ProductC4Model`
- Domain planners: `FeatureC4Planner`, `CapabilityC4Planner`, `ProductC4Planner`
- Domain validators: `C4LevelValidator`, `C4TextSanitizer`
- Port: `C4DiagramPort`
- Adapter: `C4DiagramGenerator` (updated)

## 2. File Footprint

**write:**
- `src/main/java/dev/iadev/adapter/inbound/cli/XArchPlanCapabilityCommand.java`
- `src/main/java/dev/iadev/adapter/inbound/cli/XArchPlanFeatureCommand.java`
- `src/main/java/dev/iadev/adapter/inbound/cli/XArchPlanProductCommand.java`
- `src/main/java/dev/iadev/adapter/outbound/documentation/C4DiagramGenerator.java`
- `src/main/java/dev/iadev/application/architecture/ArchitectureRefactoringUseCase.java`
- `src/main/java/dev/iadev/domain/architecture/C4LevelValidator.java`
- `src/main/java/dev/iadev/domain/architecture/C4TextSanitizer.java`
- `src/main/java/dev/iadev/domain/architecture/CapabilityC4Model.java`
- `src/main/java/dev/iadev/domain/architecture/CapabilityC4Planner.java`
- `src/main/java/dev/iadev/domain/architecture/FeatureC4Model.java`
- `src/main/java/dev/iadev/domain/architecture/FeatureC4Planner.java`
- `src/main/java/dev/iadev/domain/architecture/ProductC4Model.java`
- `src/main/java/dev/iadev/domain/architecture/ProductC4Planner.java`
- `src/main/java/dev/iadev/domain/port/output/C4DiagramPort.java`
- Test files (7 test classes)

## 3. Architecture

- Domain: `FeatureC4Model`, `CapabilityC4Model`, `ProductC4Model`, `FeatureC4Planner`, `CapabilityC4Planner`, `ProductC4Planner`, `C4LevelValidator`, `C4TextSanitizer`
- Port: `C4DiagramPort`
- Application: `ArchitectureRefactoringUseCase`
- Adapter inbound: `XArchPlanFeatureCommand`, `XArchPlanCapabilityCommand`, `XArchPlanProductCommand`
- Adapter outbound: `C4DiagramGenerator` (updated)

## 4. AC Coverage

| AC | Result |
|---|---|
| Feature input generates C4 Level 1-4 diagrams | PASS |
| Capability input generates C4 Level 1-3 diagrams | PASS |
| Product input generates C4 Level 1-2 diagrams | PASS |
| C4 text sanitization prevents malformed diagrams | PASS |
| C4 level validation enforces hierarchy constraints | PASS |
