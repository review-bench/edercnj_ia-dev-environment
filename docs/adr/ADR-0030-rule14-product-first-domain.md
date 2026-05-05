# ADR-0030 — Rule 14 Extension for Product-First Runtime Domain (EPIC-0077)

**ID:** ADR-0030  
**Status:** Accepted  
**Date:** 2026-05-04  
**Introduced by:** EPIC-0077 (Product-First Lifecycle & Planning C4 Model), story-0077-0000  
**Related rules:** Rule 14 (Project Scope Guard)

---

## Context

Rule 14 declares that `ia-dev-env` is a **CLI code generator** whose sole purpose is reading a YAML configuration file and producing the `.claude/` directory structure. It explicitly prohibits Java code that does not serve the generation pipeline, with a non-exhaustive list of forbidden categories (telemetry collection, release management, lifecycle auditing, etc.).

EPIC-0077 (Product-First Lifecycle) introduces a new planning hierarchy — `Product → Capability → Feature → Epic → Story → Task` — that determines **which** artifacts the generator produces and **how** they are composed. Specifically:

- A `Product` declaration in the project YAML controls which capability bundles are activated.
- A `Capability` maps to a set of conditional skills, rules, and templates that are included or excluded from the generated `.claude/`.
- A `Feature` maps to an epic (or epic cluster) whose planning artifacts are scoped to a specific product area.
- RNF validation configuration (`RnfGate`) is inherited by epics and stories to gate non-functional acceptance criteria.

These entities are **domain model for the generation pipeline** — they determine the shape of the generator's output. However, they do not fit the existing `domain/model/` package (which models project configuration: language, framework, build tool, database, etc.). They represent a distinct bounded context: the Product-First planning hierarchy.

Without an explicit Rule 14 amendment with a corresponding ADR, any PR introducing these classes would be evaluated against the Rule 14 scope guard and blocked during code review, creating friction and normative ambiguity for all 29 subsequent stories of EPIC-0077.

---

## Decision

Amend Rule 14 to explicitly authorize the following Java packages for the EPIC-0077 Product-First Lifecycle:

| Package | Entities | Justification |
| :--- | :--- | :--- |
| `domain/products/` | `Product`, `ProductId`, `ProductStatus` | Models the root of the planning hierarchy; determines which capability bundles are resolved during generation |
| `domain/capabilities/` | `Capability`, `CapabilityId` | Intermediate layer between Product and Feature; directly drives `CapabilityResolver` during composition |
| `domain/features/` | `Feature`, `FeatureId` | Maps to epic/epic cluster; used by `x-create-feature` and `x-internal-map-epic` to scope planning artifacts |
| `domain/planning/rnf-validation/` | `RnfValidationConfig`, `RnfGate` | Non-functional requirements gate configuration inherited by epics/stories; consumed by `x-refine-story` and `x-refine-epic` |

**Eligibility criterion for future packages under this amendment:** Code is eligible when it satisfies ALL three conditions:
1. Serves the `ia-dev-env generate` or `ia-dev-env validate` pipeline.
2. Models entities of the Product-First hierarchy required for generation of `.claude/` artifacts.
3. Is read during composition by `CapabilityResolver`, `CapabilityAwareComposer`, or their immediate collaborators.

Code that does not satisfy all three conditions remains subject to the original Rule 14 scope guard.

---

## Alternatives Considered

### Alternative 1: Treat new entities as extensions of `domain/model/`

Place `Product`, `Capability`, `Feature` inside the existing `domain/model/` package alongside `ProjectConfig`, `StackConfig`, and related records.

**Rejected because:** `domain/model/` models project-level configuration (language, framework, build tool) — a flat, static namespace. The Product-First hierarchy is a dynamic, hierarchical namespace with its own identity types (`ProductId`, `CapabilityId`, `FeatureId`) and lifecycle transitions. Mixing the two bounded contexts in a single package violates SRP and would create semantic confusion in code reviews and downstream schema evolution.

### Alternative 2: Include the Rule 14 amendment in story-0077-0001 (technical foundation)

Defer the normative amendment until the first story that introduces actual Java code.

**Rejected because:** story-0077-0001 would be in normative violation from the moment its PR is opened, before the amendment exists. Rule 14 is loaded in every conversation — a code reviewer executing `x-review-codebase` on story-0077-0001's PR without the amendment would correctly flag the new packages as scope violations. The amendment must precede the code, not accompany it.

### Alternative 3: Create a new Rule 35 for Product-First Domain Scope

Introduce a new numbered rule instead of amending Rule 14.

**Rejected because:** Rule 14 is the canonical scope guard for the `ia-dev-env` generator. A separate rule for a sub-scope of the same concern would fragment the normative contract and require engineers to consult two rules to understand what code is permitted. An amendment section within Rule 14 is more cohesive and directly traceable.

---

## Consequences

### Positive
- All 29 subsequent stories of EPIC-0077 can introduce `domain/products/`, `domain/capabilities/`, `domain/features/`, and `domain/planning/rnf-validation/` without normative conflict.
- The eligibility criterion (three conditions) creates a bounded extension — it cannot be used to justify arbitrary code additions.
- The amendment is OCP-compliant: Rule 14's existing scope guard is not modified, only extended.

### Negative / Risks
- The new packages must be maintained alongside the generation pipeline. If the Product-First Lifecycle is deprecated in a future EPIC, these packages must be explicitly removed and this ADR superseded.
- The eligibility criterion requires interpretation in edge cases — future PRs should reference this ADR when claiming authorization under the amendment.

### Mitigations
- Rule 14's "Forbidden" section remains unchanged — the amendment does not weaken the guard for non-authorized packages.
- This ADR is referenced by Rule 14 §Product-First Domain Extension so the normative chain is traceable.
- `audit-doc-freshness.sh` will detect if the ADR is removed without a corresponding Rule 14 revert.

---

## References

- Rule 14 — Project Scope Guard: `.claude/rules/14-project-scope.md`
- EPIC-0077 story-0077-0000: `ai/epics/epic-0077-product-first-lifecycle/story-0077-0000.md`
- `CapabilityResolver`: `src/main/java/dev/iadev/application/composition/CapabilityResolver.java`
- `CapabilityAwareComposer`: `src/main/java/dev/iadev/application/composition/CapabilityAwareComposer.java`
