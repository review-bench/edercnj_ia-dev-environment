# ADR-0031 — Product-First Capability Registration

**Status:** Accepted  
**Date:** 2026-05-04  
**Epic:** EPIC-0077 (Product-First Lifecycle & Planning C4 Model)  
**Story:** story-0077-0001 (TASK-0077-0001-003)  
**Supersedes:** n/a  
**See also:** ADR-0030 (Rule 14 Extension for Product-First Runtime Domain)

---

## Context

EPIC-0077 introduces a *Product-First Lifecycle* layer on top of the EPIC-0064 capability
system. A `Product` entry in the project YAML declares a curated set of capability IDs that
activate during `ia-dev-env generate` — without requiring every individual capability to be
listed explicitly in the YAML file.

Two design decisions required a dedicated record:

1. **`domain/capability/CapabilityId`** (EPIC-0064) is a complex glob-aware value object that
   supports wildcard patterns such as `data.database.*` for the `CapabilityResolver`. Reusing
   it for Product-First would couple the simpler product hierarchy to glob resolution semantics
   it does not need.

2. The Product-First capability lifecycle (which capabilities belong to which product) is a
   *domain concern* distinct from the *composition concern* (which YAMLs to include in `.claude/`).
   Keeping the two separate respects the SRP and avoids coupling `domain/products/Product.java`
   to the resolver infrastructure.

## Decision

Introduce two new types in `domain/capabilities/` (plural — distinct package from the existing
`domain/capability/` singular):

| Type | Role |
| :--- | :--- |
| `ProductCapabilityId` | Simple String-wrapped value object (no glob). |
| `ProductCapability` | Record carrying id, name, description, and universal flag. |

These types are read by the `CapabilityAwareComposer` (EPIC-0064) when a `Product` block is
present in the project YAML, via the `product-capability-transformation` capability that enables
the `Product → ResolvedCapabilitySet` expansion path (story-0077-0001, TASK-0077-0001-001).

The five Product-First capabilities are registered in `capabilities/product-first/` (YAML files)
and indexed in `capabilities/_index.yaml` under the `product-first` category.

## Rationale

### Why not reuse `domain/capability/CapabilityId`?

The existing `CapabilityId` validates its value against a CANONICAL regex and supports glob
patterns. For the Product-First catalog, capability IDs are always fully-qualified exact strings
(e.g., `product-first.c4-model-mandatory`) — glob support is unused and the validation regex
would impose an unintended constraint on how Product-First capability IDs can be named.

### Why a separate `domain/capabilities/` package?

ADR-0030 (Rule 14 extension) explicitly authorizes `domain/capabilities/` for the Product-First
domain objects. Placing these types under `domain/capability/` (EPIC-0064 package) would mix
two distinct bounded contexts — capability graph resolution (infrastructure-adjacent) and
product hierarchy modeling (pure domain).

### Why `ProductCapability` rather than reusing the YAML-parsed `CapabilityDefinition`?

`CapabilityDefinition` (EPIC-0064) is parsed from YAML frontmatter and carries `requires`,
`excludes`, `parameters`, and `fragment-slot` fields. `ProductCapability` is a minimal record
carrying only what the Product-First orchestration layer needs. Adding Product-First lifecycle
semantics to `CapabilityDefinition` would violate the SRP.

## Consequences

### Positive
- Clean separation of bounded contexts: capability graph resolution vs. product capability catalog.
- `ProductCapabilityId` is zero-dependency (standard library only), testable in isolation.
- The composition pipeline can evolve the glob resolver independently of the product hierarchy.

### Negative / Accepted trade-offs
- Two `*CapabilityId` types exist in the domain layer. The package name differentiates them
  (`domain/capability/CapabilityId` vs `domain/capabilities/ProductCapabilityId`).
- Future contributors must understand the distinction — mitigated by Javadoc cross-references
  and this ADR.

## Compliance

- Rule 03 (Coding Standards) — records ≤ 25 lines, no nulls, defensive copies.
- Rule 04 (Architecture Summary) — domain layer has zero external imports.
- Rule 05 (Quality Gates) — 13 unit tests cover all paths; BUILD SUCCESS (4732 tests total).
- Rule 14 §Product-First Domain Extension (ADR-0030) — `domain/capabilities/` is an authorized package.
