---
name: value-driven-templates
description: Full value-driven templates v2 reference — epic/story template structure, system.md lifecycle, adoption policy
requires-capabilities: []
---
# Value-Driven Templates v2 — Full Reference

> **Introduced by:** EPIC-0070. **Supersedes:** EPIC-0056.
> **ADR:** ADR-0023

## Purpose

Templates for Epics, Stories, and Architecture artifacts MUST separate **business value** from **technical architecture**. Three structural invariants:

1. `_TEMPLATE-EPIC.md` v2 — 9 business-value sections only. No Packages, Contracts-technical, SOLID Materialization.
2. `_TEMPLATE-STORY.md` v2 — 9 sections focused on value delivery. Contracts section for typed API/event contracts.
3. `_TEMPLATE-ARCHITECTURE-SYSTEM.md` (NEW) — single live document `docs/architecture/system.md` capturing system-level technical decisions.

## Template Version Detection

| Template | Version | Detection |
| :--- | :--- | :--- |
| `_TEMPLATE-EPIC.md` v1 | Legacy | Contains `## Packages — Hexagonal` section |
| `_TEMPLATE-EPIC.md` v2 | Current | Contains `## 1. Visão & Problema` as first section; no Packages section |
| `_TEMPLATE-STORY.md` v1 | Legacy | Contains `## Packages` or `## SOLID Materialization` |
| `_TEMPLATE-STORY.md` v2 | Current | Contains `## 4. Critérios de Aceite` with 4-category Gherkin mandate |

## Mandatory Sections — Epic Template v2

1. **Visão & Problema** — narrative
2. **Persona & Stakeholders** — ≥ 1 specific persona
3. **Hipótese de Valor & OKRs/KPIs** — value hypothesis in `If...then...because` form; ≥ 1 measurable KPI
4. **Alternativas Consideradas** — ≥ 2 alternatives with rejection rationale
5. **Escopo & Fora-de-Escopo** — explicit in-scope AND out-of-scope list (≥ 3 items)
6. **Riscos de Produto & Mitigações** — ≥ 1 product risk + ≥ 1 technical risk
7. **Stories** — index with value-per-story annotation
8. **Quality Gates** — DoR/DoD inherited from project
9. **Origem & Referências** — decision rationale, ADRs, AI memory references

**Forbidden in epic v2:** Packages — Hexagonal, Contratos técnicos, SOLID Materialization, Coding Constraints.

## Mandatory Sections — Story Template v2

1. **Visão** — As X, eu quero Y, garantindo Z
2. **Persona & Cenário de Uso** — specific role; not "sistema" or "usuário"
3. **Entrega de Valor** — measurable metric with unit and target
4. **Critérios de Aceite** — Gherkin with ≥ 4 mandatory categories:
   - `happy-path` — nominal success flow
   - `error/boundary` — failure and edge cases
   - `performance/SLA` — latency/throughput target (e.g., `≤ 200ms P99`)
   - `security/auth` — access control or validation scenario
5. **Contratos** — typed request/response/event schemas; no `Object` or `Map<String, Any>`
6. **Tasks** — 3-8 tasks; `task-XXXX-YYYY-NNN` format
7. **Dependências** — Blocked By / Blocks
8. **Decision Rationale** — ≥ 1 micro-template (4-line format)
9. **Refinement Verdict** — filled by `/x-refine-story`

## Adoption Policy (2-release window)

Skills `x-epic-create` and `x-story-create` emit v2 templates by default. Authors may pass `--legacy-template-v1` for up to 2 releases.

## `docs/architecture/system.md` Lifecycle

| Event | Action |
| :--- | :--- |
| Project initialization | `ia-dev-env generate` scaffolds from template with YAML auto-fill |
| Epic implementation completes | `/x-update-system-architecture` updates Decision Log |
| ADR merged | Optionally run `/x-update-system-architecture --adr ADR-NNNN` |

Sections 1-5 are auto-filled from project YAML — MUST NOT be edited manually.

## Forbidden

- Creating new Epic or Story documents using v1 template structure without `--legacy-template-v1`
- Placing hexagonal package declarations inside an Epic or Story document under v2 rules
- Editing the auto-fill sections (1-5) of `docs/architecture/system.md` directly
- Omitting the 4-category Gherkin mandate in Story AC
- Declaring an AC scenario with `performance/SLA` that has no measurable unit and target
