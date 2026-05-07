---
requires-capabilities: []
---
# Rule 14 — Project Scope Guard

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

- **Telemetry collection or analysis** — the generated `.claude/hooks/` scripts handle telemetry at runtime; the generator does not need Java telemetry classes
- **Release management** — version bumping, changelog generation, release orchestration belong in the generated skills, not in the generator itself
- **Parallelism analysis** — file footprint collision detection is a runtime concern of the orchestrating skills
- **Lifecycle auditing** — status reconciliation, lifecycle transitions are runtime concerns
- **CI validation gates** — telemetry marker linting, interactive gates auditing belong in CI scripts or generated skills
- **Checkpoint/resume for epic/story execution** — the generator runs to completion in one shot
- **Quality gate scoring** — story quality scoring is a runtime concern
- **Scope assessment** — story complexity classification is a runtime concern
- **Traceability analysis** — test-to-requirement correlation is a runtime concern
- **Schema versioning for execution state** — v1/v2 dispatch logic is a runtime concern

## Rationale

The project accumulated ~250 Java classes for runtime tools that are unrelated to code generation.
These classes inflated compile time, test suite duration, and maintenance burden.
The generated `.claude/` directory already contains skills and hooks that handle these runtime concerns — duplicating them in Java defeats the purpose of a generator.

## How to Evaluate New Code

Before adding a new package or class, ask:

1. Does this code run during `ia-dev-env generate` or `ia-dev-env validate`?
2. Does it read configuration and produce files in the `.claude/` output directory?
3. Would removing it break the generation pipeline?

If the answer to all three is NO, the code does not belong in this project.

## Product-First Domain Extension (EPIC-0077)

> **ADR:** [ADR-0030 — Rule 14 Extension for Product-First Runtime Domain](../../docs/adr/ADR-0030-rule14-product-first-domain.md)
> **Introduced by:** EPIC-0077 (Product-First Lifecycle & Planning C4 Model), story-0077-0000

The following additional packages are explicitly authorized under this amendment. They serve the `ia-dev-env generate` pipeline by modeling the Product-First planning hierarchy that drives capability resolution and composition:

| Package | Entities | Pipeline role |
| :--- | :--- | :--- |
| `domain/products/` | `Product`, `ProductId`, `ProductStatus` | Root of planning hierarchy; determines which capability bundles are resolved during generation |
| `domain/capabilities/` | `Capability`, `CapabilityId` | Intermediate layer between Product and Feature; drives `CapabilityResolver` during composition |
| `domain/features/` | `Feature`, `FeatureId` | Maps to epic/epic cluster; used by `x-create-feature` and `x-internal-map-epic` to scope planning artifacts |
| `domain/planning/rnf-validation/` | `RnfValidationConfig`, `RnfGate` | Non-functional requirements gate configuration inherited by epics/stories; consumed by `x-refine-story` and `x-refine-epic` |

**Eligibility criterion for future packages under this amendment:** Code is eligible when it satisfies ALL three conditions:
1. Serves the `ia-dev-env generate` or `ia-dev-env validate` pipeline.
2. Models entities of the Product-First hierarchy required for generation of `.claude/` artifacts.
3. Is read during composition by `CapabilityResolver`, `CapabilityAwareComposer`, or their immediate collaborators.

Code that does not satisfy all three conditions remains subject to the original Rule 14 scope guard above.

---

## Worktree Lifecycle (EPIC-0049)

Git worktrees enable parallel task / story / epic execution by creating additional working trees for the same repository. They live under `.claude/worktrees/{identifier}/` and are managed exclusively by the `x-manage-worktrees` skill.

### Directory Pattern

| Scope | Pattern | Base Branch |
| :--- | :--- | :--- |
| Task (within a story) | `.claude/worktrees/task-XXXX-YYYY-NNN/` | Parent story branch |
| Story (within an epic) | `.claude/worktrees/story-XXXX-YYYY/` | `epic/XXXX` (not `develop` — see Rule 21) |
| Epic integration | `.claude/worktrees/epic-XXXX/` | `develop` |
| Feature creation (EPIC-0065) | `.claude/worktrees/feature-XXXX-<slug>/` | `epic/XXXX` — owned by `x-create-feature`; sub-skills inherit via §3 re-entrancy |
| Feature ideation (EPIC-0065) | `.claude/worktrees/feature-ideation-<slug>/` | `develop` — owned by `x-ideate-feature`; produces spec PR to `develop` |

### Invariants

- **Creator-owned removal.** The skill that created a worktree is the only one allowed to remove it. Nested invocations reuse the existing worktree without creating a new one (ADR-0004 §D2).
- **Epic-base for parallel stories.** In `--parallel` mode, story worktrees MUST be created from `epic/XXXX`, never from `develop`. This keeps story PRs targetable at the epic branch (Rule 21, RULE-002 of EPIC-0049).
- **Failure preservation.** A worktree MUST be preserved on failure for diagnosis. Removal happens only on success, or via explicit `x-manage-worktrees cleanup` by the user.
- **One worktree per identifier.** Attempts to create a second worktree with the same identifier are no-ops that return the existing path.