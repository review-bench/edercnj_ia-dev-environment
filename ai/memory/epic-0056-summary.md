---
epic-id: EPIC-0056
slug: ra9-planning-templates
summary-version: "1.0"
created: "2026-05-03"
last-updated: "2026-05-03"

indexable: true
archived: true
superseded-by: EPIC-0070

tags: [planning, templates, governance, superseded]
capabilities-affected: []
rules-affected: []
adrs-referenced: [ADR-0023]

patterns-introduced: []
antipatterns-rejected: []

dependencies-of: [EPIC-0046]
dependencies-for: []
---
# Memory: EPIC-0056 — RA9 Templates de Planejamento (SUPERSEDED)

## Why this epic existed

Inconsistency between Epic/Story/Task planning templates: no fixed section set, no mandatory `## Decision Rationale`, no `## Packages (Hexagonal)` section, and no `## Contratos & Endpoints`. The RA9 (Rule-Aligned 9-Section) model was introduced to standardize all three levels around a fixed 9-section structure anchored to Rules (04, 05, 06, 24) and Knowledge Packs.

## Hypothesis tested

A single 9-section template model (RA9) applied to Epic, Story, and Task would eliminate inter-level inconsistency and make planning artifacts machine-auditable. **Partially confirmed, then superseded**: EPIC-0056 delivered the RA9 template structure, but the model mixed business value and technical architecture sections in the same document, which EPIC-0070 diagnosed as a structural dysfunction. EPIC-0070 superseded EPIC-0056 with v2 value-driven templates separating concerns.

## Decisions taken (with why)

1. **RA9 model with `## Packages (Hexagonal)` mandatory** — later identified as mixing levels of abstraction; deprecated in EPIC-0070.
2. **`LifecycleIntegrityAuditTest` extension** — CI-blocking audit for `RA9_SECTIONS_MISSING`, `RA9_RATIONALE_EMPTY`, `RA9_PACKAGES_MISSING`.

## Alternatives rejected (with why)

- **Per-level templates without normalization** — inconsistency between Epic/Story/Task planning artifacts; harder to audit.

## Reusable patterns produced

*(None — patterns superseded by EPIC-0070 v2 value-driven templates. See EPIC-0070 for current patterns.)*

## Anti-patterns observed

- **Mixing business value and technical architecture in same template** — `## Packages (Hexagonal)` in an Epic template conflates what the epic delivers (value) with how it's implemented (architecture). EPIC-0070 separated these concerns.

## Links

- Epic: `ai/epics/epic-0056-ra9-planning-templates/epic-0056.md` (marked `## ⛔ SUPERSEDED`)
- Superseded by: `ai/epics/epic-0070-value-driven-templates-v2/epic-0070.md`
- ADR: `docs/adr/ADR-0023-value-driven-templates.md`
- PRs: (merged into develop)
