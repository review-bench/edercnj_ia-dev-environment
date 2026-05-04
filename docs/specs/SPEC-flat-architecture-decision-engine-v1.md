# SPEC-flat-architecture-decision-engine-v1 — Flat Architecture and Architecture Decision Engine

> **Status:** Draft  
> **Author:** GitHub Copilot CLI  
> **Date:** 2026-05-04  
> **Branch:** docs/feature-flat-architecture-decision-engine

---

## System

This feature defines a practical architecture governance model for teams that want to start simple and scale structure only when the problem justifies it. The system treats Flat Architecture as the default style for new products, internal services, and bounded contexts that benefit more from clarity, speed, and low structural overhead than from early abstraction and ceremony.

In addition to the default style, the feature introduces an architecture decision engine that evaluates when a team should stay with Flat Architecture, move to a modular monolith, harden a hotspot with selective hexagonal boundaries, extract a microservice, or apply CQRS only in a measurable hotspot. The intended stakeholders are architects, tech leads, platform engineers, and delivery teams that need a disciplined but lightweight way to make architecture decisions.

---

## Scope

### Included

- Define Flat Architecture as the default architectural starting point for suitable systems and bounded contexts.
- Establish objective guardrails for feature-first modular organization, shallow package depth, limited `shared/`, and explicit module APIs.
- Provide a reusable decision engine with evaluation axes, eliminatory rules, and recommended architectural outcomes.
- Describe the expected evolution path from Flat Architecture to modular monolith, selective hexagonal design, simple microservices, and punctual CQRS.
- Prepare architecture profile outputs that can later be represented in project configuration and generation workflows.

### Excluded

- Making full hexagonal architecture the mandatory default for every project or module.
- Mapping every bounded context directly to a microservice without operational justification.
- Applying CQRS, Event Sourcing, or distributed architecture as global defaults without a proven hotspot or regulatory need.

---

## Rules

| ID | Rule | Impact |
|----|------|--------|
| RULE-001 | Flat Architecture is the default recommendation when there is no strong evidence that a heavier structure is required. | Affects project bootstrap, defaults, and initial architecture guidance. |
| RULE-002 | Modules must be organized by feature, capability, or bounded context rather than by global class type. | Affects package layout, discoverability, ownership, and cohesion. |
| RULE-003 | Extra layers, interfaces, DTOs, ports, and adapters must exist only when they produce concrete value in testability, isolation, substitutability, compliance, or clarity. | Affects implementation discipline and prevents speculative abstraction. |
| RULE-004 | `shared/` must stay small and strictly cross-cutting, with no context-specific business rules. | Affects module boundaries and reduces cross-context coupling. |
| RULE-005 | A bounded context must be treated first as a model and ownership boundary, not as an automatic deployment boundary. | Affects service extraction decisions and avoids premature microservices. |
| RULE-006 | Hexagonal design, microservices, and CQRS must be applied selectively to hotspots, unstable edges, or regulated contexts. | Affects escalation paths and concentrates complexity where it solves a real problem. |
| RULE-007 | Architectural exceptions and hardening steps must be explicit, documented, and justified by objective signals. | Affects governance, traceability, and incremental evolution. |

---

## Stories

Preliminary backlog for later decomposition in `x-create-feature`:

| # | Title | Stakeholder |
|---|-------|-------------|
| 1 | Define objective guardrails for `architecture.style: flat` | Tech Lead |
| 2 | Create a reusable architecture decision checklist with weighted axes | Software Architect |
| 3 | Implement eliminatory rules for isolation, deployment independence, unstable edges, and CQRS hotspots | Software Architect |
| 4 | Generate architecture recommendations between Flat Architecture, Modular Monolith, and selective Hexagonal design | Tech Lead |
| 5 | Provide configurable architecture profiles for `flat`, `modular-flat`, and microservice variants | Platform Engineer |
| 6 | Document how and when a flat module should evolve into a hardened hotspot or independent service | Software Architect |
| 7 | Enforce guardrails that keep `shared/` limited to transversal concerns | Developer |
| 8 | Define documentation and governance requirements for architectural exceptions | Tech Lead |

---

## DoR / DoD

### Definition of Ready

- [ ] Decision axes, eliminatory rules, and architectural outcomes are clearly named and unambiguous.
- [ ] The intended users of the decision engine and their decision points in the workflow are identified.
- [ ] The baseline Flat Architecture guardrails are specific enough to become checklists, rules, or generator options.
- [ ] The initial profile set and its boundaries are explicit enough to prevent scope drift during decomposition.

### Definition of Done

- [ ] The feature can recommend Flat Architecture, Modular Monolith, selective Hexagonal design, microservice extraction, or punctual CQRS using explicit criteria.
- [ ] Flat Architecture guardrails are operational, including limits for `shared/`, module dependencies, and shallow structure.
- [ ] The resulting decision framework is reusable across new systems and bounded contexts instead of being tied to one document only.
- [ ] The architecture profiles preserve simplicity by default and introduce heavier patterns only when justified by measurable signals.

---

## Risks

| Risk | Impact | Mitigation |
|------|--------|------------|
| Flat Architecture is interpreted as “no architecture” instead of “minimal intentional architecture.” | High | Define non-negotiable guardrails for module ownership, public APIs, shallow structure, and dependency rules. |
| `shared/` becomes a dumping ground for unclear responsibilities. | High | Restrict `shared/` to transversal concerns only and make context business rules stay inside their owning modules. |
| Teams use the decision engine as a cosmetic checklist and skip necessary refactors or hardening. | Medium | Require explicit justification, exception recording, and signal-based promotion from flat modules to hardened hotspots or services. |
