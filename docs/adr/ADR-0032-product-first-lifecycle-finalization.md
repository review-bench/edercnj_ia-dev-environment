# ADR-0032 — Product-First Lifecycle Finalization (EPIC-0077)

**ID:** ADR-0032  
**Status:** Accepted  
**Date:** 2026-05-05  
**Epic:** EPIC-0077 (Product-First Lifecycle & Planning C4 Model)  
**Story:** story-0077-0028  
**See also:** ADR-0030, ADR-0031

---

## Context

EPIC-0077 introduced the Product-First hierarchy incrementally across multiple stories: ideation promotion, product creation, capability decomposition, feature decomposition, Feature → Epic derivation, Feature → Story derivation, C4 mandatory coverage, RNF inheritance gates, and CI audits. By the end of the epic, the implementation existed across many packages and templates, but the **final normative shape** of the lifecycle was still fragmented across story-level artifacts.

Without a single consolidation ADR, reviewers and future contributors would need to reconstruct the intended lifecycle from dozens of incremental changes. That would make it hard to answer basic governance questions such as:

- which hierarchy is canonical for new planning work;
- which gates are mandatory for Product-First artifacts;
- which artifacts are generated vs. authored at each stage;
- how C4, RNF inheritance, pentest planning, and audit scripts interact.

## Decision

The Product-First Lifecycle is finalized as the canonical planning chain for EPIC-0077 and successor epics. The following decisions are now normative as a **single cohesive contract**:

1. **Canonical hierarchy:** `Ideation → Product → Capability → Feature → Epic → Story → Task`.
2. **Normative state version:** every Product-First epic persists `flowVersion: "5"` with `productFirstLifecycle: true`.
3. **Upstream artifacts:** Product, Capability, and Feature artifacts are the authoritative upstream evidence for downstream derivation and audits.
4. **Feature-driven backlog generation:** `x-epic-create --from-feature` and `x-story-create --from-feature --epic-id` are the public Product-First entry points for derived backlog creation.
5. **C4 mandatory coverage:** derived epic/story artifacts must expose `C4 Context`, `C4 Container`, `C4 Component`, and `C4 Code` sections so the lifecycle remains auditable at Camada 2.
6. **RNF inheritance contract:** inherited RNFs flow from Product/Capability into derived Epic/Story artifacts; no-relax semantics must be preserved when lineage is materialized.
7. **Security hardening evidence:** every security-relevant Product-First story requires pentest planning evidence to satisfy audit coverage.
8. **Layered verification:** Product-First lifecycle validity is enforced cooperatively by domain/application validation, artifact generation tests, smoke tests, and Camada 2 audit scripts.
9. **Task visibility in derived stories:** Feature-derived stories must materialize executable task shells so the chain reaches task granularity even before `/x-plan-task` expands them further.

## Rationale

These decisions were already present in code and tests by the end of EPIC-0077, but they were distributed across multiple stories and partial ADRs. Consolidating them in one ADR reduces ambiguity, gives Rule 19 a clear normative companion, and makes future regressions easier to review because engineers can compare any new change against one stable final-state document.

## Alternatives Considered

### Alternative 1: Leave the epic finalized only through story markdown

**Rejected because:** story files are operational planning artifacts, not the long-lived architectural source of truth. They are too granular and too numerous to serve as the final normative reference for the lifecycle.

### Alternative 2: Expand ADR-0030 or ADR-0031 instead of creating a new ADR

**Rejected because:** ADR-0030 scopes the Rule 14 domain extension and ADR-0031 scopes capability registration. Neither is the right container for the full end-to-end lifecycle, audits, and gating contract that emerged later in the epic.

## Consequences

### Positive

- Future Product-First work has one final architectural reference for lifecycle semantics.
- Rule 19 can point to a stable ADR when describing `flowVersion: "5"` as the normative path.
- Smoke and audit regressions become easier to interpret because the expected contract is explicit.

### Negative / Accepted trade-offs

- Some details remain duplicated between this ADR, Rule 19, and story artifacts; keeping them aligned is now a documentation maintenance responsibility.
- The lifecycle is more explicit and therefore more rigid; future changes should supersede this ADR rather than silently drift from it.

## References

- Rule 19 — `src/main/resources/targets/claude/rules/19-backward-compatibility.md`
- ADR-0030 — `docs/adr/ADR-0030-rule14-product-first-domain.md`
- ADR-0031 — `docs/adr/ADR-0031-product-first-capability-registration.md`
- EPIC-0077 — `ai/epics/epic-0077-product-first-lifecycle/`
