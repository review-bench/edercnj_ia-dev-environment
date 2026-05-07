---
name: project-scope
description: Project scope guard — allowed code layers, forbidden runtime concerns, Product-First extension, worktree lifecycle
requires-capabilities: []
---
# Project Scope Guard — Full Reference

## Rule

This project is a **CLI code generator**. Its sole purpose is:

1. Read a YAML configuration file describing a project's tech stack
2. Generate the `.claude/` directory structure with the appropriate skills, rules, agents, hooks, and settings
3. Copy and assemble resource files according to the user's configuration

## Allowed Code

| Layer | Allowed |
|-------|---------|
| `cli/` | CLI commands (`generate`, `validate`) and display utilities |
| `application/assembler/` | Assemblers that produce `.claude/` artifacts |
| `config/` | YAML configuration loading and context building |
| `domain/model/` | Data models representing project configuration |
| `domain/port/` | Input/output port interfaces for generation |
| `domain/service/` | Domain services orchestrating generation |
| `domain/stack/` | Stack resolution and validation |
| `template/` | Template engine integration |
| `infrastructure/adapter/` | Concrete implementations of output ports |
| `util/` | Path utilities, resource extraction |
| `exception/` | Core exception types |

## Forbidden

Adding Java code that does NOT serve the CLI generation pipeline:

- Telemetry collection or analysis — handled by generated `.claude/hooks/` scripts
- Release management — handled by generated skills
- Parallelism analysis — runtime concern
- Lifecycle auditing — runtime concern
- CI validation gates — handled by CI scripts or generated skills
- Checkpoint/resume for epic/story execution — runtime concern
- Quality gate scoring — runtime concern
- Scope assessment — runtime concern
- Traceability analysis — runtime concern
- Schema versioning for execution state — runtime concern

## How to Evaluate New Code

Before adding a new package or class, ask:

1. Does this code run during `ia-dev-env generate` or `ia-dev-env validate`?
2. Does it read configuration and produce files in the `.claude/` output directory?
3. Would removing it break the generation pipeline?

If the answer to all three is NO, the code does not belong in this project.

## Product-First Domain Extension (EPIC-0077)

The following packages are explicitly authorized:

| Package | Entities | Pipeline role |
| :--- | :--- | :--- |
| `domain/products/` | `Product`, `ProductId`, `ProductStatus` | Root of planning hierarchy; determines capability bundles |
| `domain/capabilities/` | `Capability`, `CapabilityId` | Intermediate layer; drives `CapabilityResolver` during composition |
| `domain/features/` | `Feature`, `FeatureId` | Maps to epic/epic cluster; used by `x-create-feature` and `x-internal-map-epic` |
| `domain/planning/rnf-validation/` | `RnfValidationConfig`, `RnfGate` | Non-functional requirements gate configuration |

**Eligibility criterion:** Code must satisfy ALL three conditions:
1. Serves the `ia-dev-env generate` or `ia-dev-env validate` pipeline
2. Models entities of the Product-First hierarchy required for `.claude/` artifact generation
3. Is read during composition by `CapabilityResolver`, `CapabilityAwareComposer`, or immediate collaborators

## Worktree Lifecycle (EPIC-0049)

Git worktrees enable parallel task / story / epic execution. They live under `.claude/worktrees/{identifier}/`.

| Scope | Pattern | Base Branch |
| :--- | :--- | :--- |
| Task | `.claude/worktrees/task-XXXX-YYYY-NNN/` | Parent story branch |
| Story | `.claude/worktrees/story-XXXX-YYYY/` | `epic/XXXX` |
| Epic integration | `.claude/worktrees/epic-XXXX/` | `develop` |
| Feature creation | `.claude/worktrees/feature-XXXX-<slug>/` | `epic/XXXX` |
| Feature ideation | `.claude/worktrees/feature-ideation-<slug>/` | `develop` |

### Invariants

- **Creator-owned removal.** Only the creating skill may remove a worktree.
- **Epic-base for parallel stories.** Story worktrees MUST use `epic/XXXX` as base, not `develop`.
- **Failure preservation.** Preserve on failure for diagnosis; remove only on success.
- **One worktree per identifier.** Idempotent creation.
