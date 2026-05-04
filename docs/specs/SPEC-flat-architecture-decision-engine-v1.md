# SPEC-flat-architecture-decision-engine-v1 — Product Architecture Profiles and Decision Engine

> **Status:** Draft  
> **Author:** GitHub Copilot CLI  
> **Date:** 2026-05-04  
> **Branch:** docs/feature-flat-architecture-decision-engine

---

## System

This feature defines a governance model for **software product architecture** only. It explicitly excludes solution architecture, enterprise landscape design, platform architecture, and organization-wide integration blueprints. The goal is to classify how much internal architecture a software product, module, or service should carry, and to evolve that decision through measurable complexity instead of ideology, fashion, or framework bias.

The model separates two independent axes. The first axis is **product topology**: single application, modular monolith, service federation, or microservice. The second axis is **architecture intensity**: **Flat Architecture** for lean internals, **Structured Architecture** for balanced explicit structure, and **Hardened Architecture** for strongly isolated and highly governed internals. Because the axes are independent, a microservice may be Flat, Structured, or Hardened internally, and the same is true for modules inside a modular monolith.

To make the model operational, the feature also introduces **pattern packs** and **rules packs**. Hexagonal, Tactical DDD, CQRS, Event Sourcing, and similar approaches are treated as selective packs that can be attached to a topology and intensity combination when measurable signals justify them. SOLID and related design constraints remain cross-cutting rules rather than topology choices. A software product complexity score then drives the recommendation engine with explicit dimensions, thresholds, and promotion signals.

---

## Scope

### Included

- Define three formal architecture intensity tiers: Flat Architecture, Structured Architecture, and Hardened Architecture.
- Separate topology decisions from architecture intensity so that monoliths, modular monoliths, and microservices can each use different internal architecture profiles.
- Establish explicit module standards for each tier, including allowed structure depth, public module API shape, permitted abstractions, and shared-kernel constraints.
- Define pattern-pack contracts for Hexagonal, Tactical DDD, CQRS, Event Sourcing, and related approaches, including required building blocks, optional elements, and anti-goals.
- Define cross-cutting rules packs for SOLID and similar design principles that apply independently of topology.
- Introduce a measurable **Software Product Complexity Score (SPCS)** with weighted dimensions, thresholds, and recommendation logic.
- Prepare configuration-ready profile outputs and decision artifacts that can later be represented in generator rules, YAML, or policy checklists.

### Excluded

- Treating solution architecture, enterprise architecture, or platform architecture as part of this model.
- Making Hexagonal, DDD, Microservices, CQRS, or Event Sourcing mandatory defaults for every product or module.
- Using topology names such as "microservice" or "monolith" as a proxy for how inflated or sophisticated the internal architecture must be.

---

## Rules

| ID | Rule | Impact |
|----|------|--------|
| RULE-001 | This model governs software product architecture only; it does not define solution architecture, platform architecture, or enterprise integration maps. | Prevents scope dilution and keeps recommendations focused on application internals. |
| RULE-002 | Architecture selection must use two independent axes: product topology and architecture intensity. | Prevents false equivalence between deployment shape and internal design complexity. |
| RULE-003 | The formal intensity tiers are Flat Architecture, Structured Architecture, and Hardened Architecture. | Creates a stable vocabulary for recommendation, governance, and profile definition. |
| RULE-004 | Microservices, modular monoliths, and single-deploy systems may each use Flat, Structured, or Hardened internals depending on measured need. | Allows combinations such as flat microservices, structured monoliths, or hardened modules. |
| RULE-005 | Each intensity tier must define a mandatory module standard covering naming, package depth, public API boundaries, shared-kernel constraints, and abstraction budget. | Makes module structure explicit and comparable across products and teams. |
| RULE-006 | Pattern packs such as Hexagonal, Tactical DDD, CQRS, and Event Sourcing must declare required components, optional components, allowed contexts, promotion triggers, and anti-patterns. | Makes each approach operational instead of rhetorical or over-applied. |
| RULE-007 | SOLID and similar design principles are cross-cutting rules packs, not topology choices and not architecture tiers by themselves. | Prevents mixing design discipline with deployment or layering decisions. |
| RULE-008 | The Software Product Complexity Score (SPCS) must be computed from weighted product factors such as domain rules density, workflow coupling, integration volatility, compliance/audit pressure, runtime criticality, scale asymmetry, team autonomy, deployment independence, channel divergence, and read/write asymmetry. | Creates measurable input for architecture recommendations. |
| RULE-009 | Recommendation logic must first calculate the intensity tier from SPCS and eliminatory conditions, then evaluate the appropriate topology separately. | Produces clearer guidance and avoids inflating architecture because of one isolated signal. |
| RULE-010 | Architectural promotion, hardening, and exceptions must be documented with explicit signals, thresholds, and rationale. | Preserves traceability, reviewability, and disciplined evolution over time. |

---

## Stories

Preliminary backlog for later decomposition in `x-create-feature`:

| # | Title | Stakeholder |
|---|-------|-------------|
| 1 | Define the formal taxonomy for Flat, Structured, and Hardened architecture intensity tiers | Software Architect |
| 2 | Separate product topology choices from internal architecture intensity in the decision engine | Tech Lead |
| 3 | Define the module standard for Flat Architecture, including shallow capability modules and low abstraction budget | Developer |
| 4 | Define the module standard for Structured Architecture, including explicit module APIs, selective ports, and balanced layering | Software Architect |
| 5 | Define the module standard for Hardened Architecture, including strong boundaries, audited seams, and higher governance | Software Architect |
| 6 | Create a pattern-pack contract template for Hexagonal, Tactical DDD, CQRS, Event Sourcing, and similar approaches | Platform Engineer |
| 7 | Define cross-cutting rules packs for SOLID and related design constraints | Tech Lead |
| 8 | Implement the Software Product Complexity Score with weighted dimensions, thresholds, and calibration examples | Software Architect |
| 9 | Generate recommendation outputs that independently suggest topology and intensity tier | Platform Engineer |
| 10 | Define promotion rules from Flat to Structured to Hardened and from single deploy to modular or distributed topology | Tech Lead |
| 11 | Document how architectural exceptions, module deviations, and specialized packs are recorded and reviewed | Tech Lead |

---

## DoR / DoD

### Definition of Ready

- [ ] Topology and architecture intensity are explicitly separated and named without ambiguity.
- [ ] The proposed intensity-tier names and semantics are accepted by stakeholders.
- [ ] The module-standard template is defined for both architecture tiers and pattern packs.
- [ ] The SPCS dimensions, weights, thresholds, and calibration method are explicit enough to be reviewed objectively.
- [ ] The intended operators of the recommendation engine and governance flow are identified.

### Definition of Done

- [ ] The feature distinguishes software product architecture from solution architecture in both language and recommendation logic.
- [ ] The recommendation engine outputs topology and architecture intensity separately, with valid combinations such as flat microservices or hardened monolith modules.
- [ ] Flat, Structured, and Hardened Architecture each have explicit module standards, abstraction budgets, and promotion signals.
- [ ] Pattern packs and rules packs are defined with required elements, allowed contexts, and anti-pattern boundaries.
- [ ] The SPCS is measurable, reviewable, and linked to explicit thresholds and recommendation outcomes.

---

## Risks

| Risk | Impact | Mitigation |
|------|--------|------------|
| Teams confuse topology with architecture intensity and assume that every microservice must be heavily engineered internally. | High | Separate topology and intensity in the model, examples, and recommendation output so combinations remain explicit. |
| Pattern packs and rules packs become bureaucratic catalogs that reintroduce architectural inflation. | High | Require each pack to declare when it should not be used, what problem it solves, and what signals justify promotion. |
| The complexity score becomes subjective or easy to game. | Medium | Use weighted dimensions, calibration examples, evidence-based scoring, and explicit review criteria for each threshold. |
