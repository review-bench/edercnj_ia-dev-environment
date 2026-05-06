---
requires-capabilities: []
---
# Rule 30 — Value-Driven Templates

> **Related:** Rule 04 (Architecture Summary), Rule 22 (Skill Visibility), Rule 28 (Capability Frontmatter Contract).
> **Introduced by:** EPIC-0070 (Value-Driven Templates v2).
> **Supersedes:** EPIC-0056 (RA9 Standardized Planning Templates — marked SUPERSEDED).
> **Cross-link:** Rule 31 (Documentation Freshness Gate, EPIC-0071) — complementary; governs temporal enforcement of doc updates; Rule 30 governs structure.
> **ADR:** [ADR-0023 — Value-Driven Templates v2](../../docs/adr/ADR-0023-value-driven-templates.md).
> **Capability:** `governance.value-driven-templates`.

## Purpose

Templates for Epics, Stories, and Architecture artifacts MUST separate **business value** from **technical architecture**. Before EPIC-0070, templates mixed value-level narrative (problem, persona, hypothesis, alternatives) with hexagonal package declarations in the same sections, forcing authors to choose between a shallow value document or a technical-heavy epic that did not communicate business rationale.

Rule 30 enforces three structural invariants:

1. **`_TEMPLATE-EPIC.md` v2** — 9 business-value sections only. No Packages, Contracts-technical, SOLID Materialization sections. Technical architecture lives in `docs/architecture/system.md`.
2. **`_TEMPLATE-STORY.md` v2** — 9 sections focused on value delivery. Contracts section is for typed API/event contracts (input for spec sync), not hexagonal structure.
3. **`_TEMPLATE-ARCHITECTURE-SYSTEM.md` (NEW)** — single live document `docs/architecture/system.md` that captures the system-level technical decisions auto-filled from the project YAML and updated incrementally per epic via `/x-update-system-architecture`.

## Template Version Detection

| Template | Version | Detection |
| :--- | :--- | :--- |
| `_TEMPLATE-EPIC.md` v1 | Legacy (EPIC-0056 era) | Contains `## Packages — Hexagonal` section |
| `_TEMPLATE-EPIC.md` v2 | This rule | Contains `## 1. Visão & Problema` as first section; no Packages section |
| `_TEMPLATE-STORY.md` v1 | Legacy | Contains `## Packages` or `## SOLID Materialization` |
| `_TEMPLATE-STORY.md` v2 | This rule | Contains `## 4. Critérios de Aceite` with 4-category Gherkin mandate |
| `_TEMPLATE-ARCHITECTURE-SYSTEM.md` | New (Rule 30) | File exists at `docs/architecture/system.md` in project |

## Mandatory Sections — Epic Template v2

1. **Visão & Problema** — narrative; 3-8 paragraphs recommended; no section size cap
2. **Persona & Stakeholders** — ≥ 1 specific persona (not "sistema")
3. **Hipótese de Valor & OKRs/KPIs** — value hypothesis in `If...then...because` form; ≥ 1 measurable KPI
4. **Alternativas Consideradas** — ≥ 2 alternatives with rejection rationale
5. **Escopo & Fora-de-Escopo** — explicit in-scope list AND out-of-scope list (≥ 3 out-of-scope items)
6. **Riscos de Produto & Mitigações** — ≥ 1 product risk + ≥ 1 technical risk
7. **Stories** — index with value-per-story annotation; blocks/blocked-by
8. **Quality Gates** — DoR/DoD inherited from project, with optional epic-level additions
9. **Origem & Referências** — decision rationale, links, ADRs, AI memory references

**Forbidden in epic template v2:** Packages — Hexagonal, Contratos técnicos, SOLID Materialization, Coding Constraints, Observabilidade técnica (all migrate to `system.md`).

## Mandatory Sections — Story Template v2

1. **Visão** — As X, eu quero Y, garantindo Z + narrative context
2. **Persona & Cenário de Uso** — specific role; not "sistema" or "usuário"
3. **Entrega de Valor** — measurable metric with unit and target
4. **Critérios de Aceite** — Gherkin with ≥ 4 mandatory categories:
   - `happy-path` — nominal success flow
   - `error/boundary` — failure and edge cases
   - `performance/SLA` — latency/throughput target
   - `security/auth` — access control or validation scenario
5. **Contratos** — typed request/response/event schemas; no `Object` or `Map<String, Any>`
6. **Tasks** — 3-8 tasks; `task-XXXX-YYYY-NNN` format
7. **Dependências** — Blocked By / Blocks
8. **Decision Rationale** — ≥ 1 micro-template (4-line format)
9. **Refinement Verdict** — filled by `/x-refine-story` (EPIC-0069, Rule 29)

## Adoption Policy (Rule 19 — 2-release window)

Skills `x-epic-create` and `x-story-create` emit v2 templates by default starting in the release that ships EPIC-0070. Authors who need to continue v1 behavior may pass `--legacy-template-v1` for up to 2 releases, after which the flag is removed.

Measurement: `audit-template-version.sh` (story-0070-0008) reports the percentage of new epics/stories using v2; target ≥ 90% adoption after 1 release.

## `docs/architecture/system.md` Lifecycle

| Event | Action |
| :--- | :--- |
| Project initialization | `ia-dev-env generate` scaffolds `docs/architecture/system.md` from `_TEMPLATE-ARCHITECTURE-SYSTEM.md` with YAML auto-fill |
| Epic implementation completes | `/x-update-system-architecture` updates the Decision Log section + any YAML-driven fields that changed |
| ADR merged | Operator optionally runs `/x-update-system-architecture --adr ADR-NNNN` to surface the decision in system.md |

Sections 1-5 of system.md (Stack, Persistência, Comunicação, Observabilidade, Resilience) are auto-filled from the project YAML on generation and MUST NOT be edited manually — edit the source YAML and regenerate. Sections 6-11 (Performance Budget, Security Baseline, Dependency Policy, Doc Targets, Integrations, Decision Log) are manually maintained.

## Forbidden

- Creating new Epic or Story documents using v1 template structure (Packages section, SOLID Materialization) without `--legacy-template-v1` flag.
- Placing hexagonal package declarations inside an Epic or Story document under v2 rules.
- Editing the auto-fill sections (1-5) of `docs/architecture/system.md` directly — modify source YAML.
- Omitting the 4-category Gherkin mandate in Story AC (happy-path / error-boundary / performance-SLA / security-auth).
- Declaring an AC scenario with `performance/SLA` that has no measurable unit and target (e.g., "should be fast" is forbidden; "≤ 200ms P99" is required).

## Audit

`scripts/audit-template-version.sh` (story-0070-0008) scans `ai/epics/` for new epic/story markdown files:

- **Pass:** File uses v2 structure (contains required v2 section headers).
- **Warn:** File uses v1 structure AND has `--legacy-template-v1` annotation in metadata.
- **Fail:** File uses v1 structure with no annotation (`TEMPLATE_VERSION_VIOLATION`, exit 1).

Exit codes follow Rule 26 §Standardized matrix (0 = OK, 1 = violation, 2 = operational error).

Self-check: `audit-template-version.sh --self-check` verifies this rule file exists and `governance/baselines/template-version-baseline.txt` is present.

---

> **Catalogado em:** [`docs/audit-gates-catalog.md`](../../docs/audit-gates-catalog.md)