# ADR-0023 — Value-Driven Templates v2

**Status:** Accepted
**Date:** 2026-04-30
**Epic:** EPIC-0070 (Value-Driven Templates v2)
**Supersedes:** EPIC-0056 (RA9 Standardized Planning Templates)

## Context

Epic and Story templates in this repository (as of EPIC-0056 planning) suffered three intertwined dysfunctions:

1. **Value/architecture mix** — Templates forced authors to write business value context (problem, persona, hypothesis) in the same structure as technical declarations (hexagonal packages, SOLID materialization, coding constraints). Authors either wrote a shallow value document or a technical-heavy epic — no template supported both well.

2. **Dispersed system architecture** — Each epic re-declared its hexagonal package structure, but the system as a whole — chosen stack, database, communication protocol, observability, resilience, performance budget, security baseline — lived dispersed across Rule 04, the project YAML, and fragments of past epics. New contributors reconstructed this map by reading 60+ files.

3. **3-5 sentence cap loses irreplaceable narrative** — The prior "concise description" constraint meant hypothesis, alternatives considered, strategic trade-offs, and risk mitigation were not recorded. Six months later neither team members nor LLMs could reconstruct why decisions were made.

EPIC-0056 (RA9) attempted to standardize 9 fixed sections but maintained the value/architecture mix. Story-0070-0001 formally supersedes EPIC-0056.

## Decision

### D-1: Split epic/story templates into value-only + architecture-system document

- **`_TEMPLATE-EPIC.md` v2** — 9 sections, all business-value oriented. No Packages, SOLID, Coding Constraints sections.
- **`_TEMPLATE-STORY.md` v2** — 9 sections, value delivery focused. Contracts section is for typed API contracts (not hexagonal structure).
- **`_TEMPLATE-ARCHITECTURE-SYSTEM.md` (NEW)** — Template for `docs/architecture/system.md`, a single live document capturing system-level technical decisions. Sections 1-5 auto-filled from project YAML; sections 6-11 manually maintained per epic.

### D-2: Rule 30 (not merged with EPIC-0071 Rule 31) — Rule separada

**D-R7 criterion applied:** measured invariant overlap between:
- EPIC-0070 invariants: template structure (what content goes where), adoption enforcement, v2 section mandate
- EPIC-0071 invariants: documentation freshness gate (docs updated when code changes, temporal staleness detection)

**Overlap measured: < 30%** — distinct subject matters:
- Rule 30 governs *structural* content of templates (value vs. architecture separation, mandatory sections)
- Rule 31 governs *temporal* enforcement (docs must be updated when relevant code changes)

**Decision: two separate rules**, each with a cross-reference to the other. Shared capability: none — each epic declares its own capability (`governance.value-driven-templates` and `governance.doc-as-dod` respectively).

### D-3: `audit-template-version.sh` audit gate

New CI script `audit-template-version.sh` (story-0070-0008, Rule 26-compliant) detects v1 templates in new epics/stories and fails the build with `TEMPLATE_VERSION_VIOLATION` unless annotated with `--legacy-template-v1`.

### D-4: SUPERSEDED marker format for EPIC-0056 (D-R9)

Format: `## ⛔ SUPERSEDED — Substituído por EPIC-0070` block inserted after the H1 title, before metadata fields. Content:
```markdown
> **Substituído por:** [EPIC-0070 (Value-Driven Templates v2)](../epic-0070-value-driven-templates-v2/epic-0070.md)
> **Data:** 2026-04-30
> **Razão:** EPIC-0056 padronizava 9 seções mas mantinha mistura valor/arquitetura. EPIC-0070 resolve as 3 disfunções (valor/técnica, arquitetura dispersa, narrativa perdida) com template separado + `docs/architecture/system.md` vivo.
> **PRs/branches em aberto sobre EPIC-0056:** devem ser fechados com referência a EPIC-0070.
```

History of EPIC-0056 is preserved (non-destructive marker — no content deleted).

### D-5: Adoption policy (Rule 19 — 2-release deprecation window)

Skills `x-epic-create` and `x-story-create` default to v2. `--legacy-template-v1` flag supported for 2 releases, then removed. Measurement via `audit-template-version.sh`.

## Consequences

**Positive:**
- Epics become readable by non-technical stakeholders without losing rigor.
- System architecture is captured once at the system level (`system.md`), not re-litigated per epic.
- LLMs and new developers recover strategic rationale by reading a single markdown file.
- Story ACs gain a 4-category Gherkin mandate (happy/error/performance/security) — enforced by Rule 29 refinement gate.

**Negative:**
- Migration effort: existing v1 epics/stories require `x-template-migrate` (story-0070-0007) for full v2 adoption.
- Authors must learn two template files (epic + story) instead of one monolithic template.
- `docs/architecture/system.md` requires initial scaffolding on project setup — `ia-dev-env generate` handles this but requires YAML completeness.

**Neutral:**
- EPIC-0056 work (RA9 planning) is not lost — insights incorporated into v2 template design.
- Rule 30 and Rule 31 are separate — no combined "governance omnibus rule".
